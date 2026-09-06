package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.mindrecord.presentation.reporting.MindRecordFailureStage
import com.afternote.feature.mindrecord.presentation.reporting.recordMindRecordFailure
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import com.afternote.feature.mindrecord.presentation.util.toWireContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 일기 작성/이어쓰기 ViewModel.
 *
 * - 신규 작성: `POST /diary` (+ 선택한 수신자 ID 를 `receiverIds` 로 함께 전송).
 * - 수정: 라우트의 `recordId` 로 해당 달 목록에서 항목을 찾아 프리필하고, 저장 시
 *   `PATCH /diary/{diaryId}` 로 수정한다. 임시저장 이어쓰기(`isDraft=true`)와 정식 기록
 *   수정(`isDraft=false`)이 같은 경로를 쓰며, 조회하는 목록의 `draftOnly` 만 다르다 (#582).
 */
@HiltViewModel
class DiaryWriteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: DiaryRepository,
        private val photoUploadRepository: PhotoUploadRepository,
        private val userRepository: UserReceiverRepository,
        private val draftLoader: LoadMindRecordDraftsUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<MindRecordRoute.DiaryWriteRoute>()

        /** 수정 대상 일기 ID. 임시저장 이어쓰기와 정식 기록 수정이 같은 값을 쓴다 (#582). */
        private val editingDiaryId: Long? = route.recordId

        private val _uiState = MutableStateFlow(DiaryWriteUiState())
        val uiState: StateFlow<DiaryWriteUiState> = _uiState.asStateFlow()

        init {
            // 이어쓰기(임시저장)일 때만 «이어쓰는 중» 으로 표시한다 — 정식 기록 수정은 아니다 (#582).
            if (editingDiaryId != null && route.isDraft) {
                _uiState.update { it.copy(isEditingDraft = true) }
            }
            loadReceivers()
            loadDraftCount()
            // 라우트가 draftId/draftYearMonth → recordId/yearMonth/isDraft 로 일반화됐다 (#582).
            editingDiaryId?.let { loadExisting(it, route.yearMonth, route.isDraft) }
        }

        /** 툴바 카운트는 화면 장식이라 실패해도 화면을 막지 않고 '모름' 으로 남긴다. */
        private fun loadDraftCount() {
            viewModelScope.launch {
                draftLoader.count().onSuccess { count ->
                    _uiState.update { it.copy(draftCount = count) }
                }
            }
        }

        fun onTitleChanged(value: String) {
            _uiState.update { it.copy(title = value) }
        }

        fun onContentChanged(value: String) {
            _uiState.update { it.copy(content = value) }
        }

        /**
         * 날짜 행에서 고른 기록일을 싣는다 (#1008).
         *
         * **미래 날짜는 받지 않는다.** 서버가 400(code 2101)으로 거절하므로, 상태에 넣으면
         * 사용자는 저장을 눌러 봐야 실패를 안다. 고르는 순간 사유와 함께 막는다.
         */
        fun onDateSelected(date: LocalDate) {
            if (date.isAfter(LocalDate.now())) {
                _uiState.update {
                    it.copy(dateError = UiText.Resource(R.string.mindrecord_error_diary_future_date))
                }
                return
            }
            _uiState.update { it.copy(date = date, isDateChosen = true, dateError = null) }
        }

        fun onMoodSelected(mood: TodayMood) {
            _uiState.update { it.copy(mood = mood) }
        }

        fun onReceiverToggled(receiverId: Long) {
            _uiState.update {
                val selected = it.selectedReceiverIds
                it.copy(
                    selectedReceiverIds =
                        if (receiverId in selected) selected - receiverId else selected + receiverId,
                )
            }
        }

        // 이번 작성에서 업로드한 `fileUrl` → 서버가 준 `fileKey`. 키를 URL 에서 역산하지 않는다 —
        // presigned 응답이 준 값을 그대로 들고 있다가 제출 직전에 치환한다 (toWireContent, #1125).
        // SavedStateHandle 에 실어 프로세스 사망을 건너뛴다 — 에디터 본문이 살아 돌아오는데
        // 표만 비면 전체 URL 이 그대로 서버로 간다 (#1125 리뷰).
        private val uploadedFileKeysByUrl = UploadedFileKeys(savedStateHandle)

        /**
         * 에디터에서 고른 **미디어**(사진·음성·파일)를 presigned URL 로 업로드하고 **미리보기에
         * 쓸 URL** 을 반환한다 (실패 시 null).
         *
         * 저장 시 본문에 나가는 값은 이 URL 이 아니다 — 서버는 `img src` 에서 fileKey 를 기대하고
         * 전체 URL 을 받으면 호스트를 한 번 더 붙여 403 이 된다. 그래서 받은 URL 을 기억해 두고
         * 제출 직전에 키로 바꾼다 ([toWireContent]). 미리보기는 전체 URL 이라야 뜬다 (#1016).
         *
         * **업로드 결과를 «대표 이미지» 로 따로 들지 않는다** (#1195). 종전에는 첫 업로드 URL 을
         * 화면 상태의 `imageUrl` 로 집었는데, 조건이 «첫 번째» 뿐이라 첨부가 미디어 전체로 넓어진
         * 뒤로는 음성을 먼저 붙이면 그 자리에 `.m4a` 가 실렸다. 게다가 그 값을 **읽는 곳이 없었다** —
         * `DiaryCreateRequest` 계약은 `[title, content, isDraft, todayMood, receiverIds]` 뿐이고
         * 화면도 그 필드를 그리지 않는다. 데일리질문은 같은 이유로 이미 상태에서 걷어 뒀다.
         *
         * 본문에 남는 이미지의 출처는 이 필드가 아니라 `content` 의 `img` 태그다 (#549).
         */
        suspend fun uploadMedia(uriString: String): String? {
            // 실패 문구의 수명은 «다음 업로드 시작까지» 다 — 화면에 걷는 수단이 따로 없고,
            // 걷는 함수만 두면 호출부 0건인 죽은 코드가 된다 (#1019 리뷰 지적).
            _uiState.update { it.copy(isUploadingImage = true, imageUploadError = null) }
            return photoUploadRepository
                .upload(uriString = uriString, directory = MIND_RECORD_UPLOAD_DIRECTORY)
                .onSuccess { uploaded ->
                    // 제출 직전 fileKey 로 바꿀 대상이다 (#1016).
                    uploadedFileKeysByUrl[uploaded.fileUrl] = uploaded.fileKey
                    _uiState.update { it.copy(isUploadingImage = false) }
                }.onFailure { e ->
                    // 첨부가 빠진 채 저장이 이어질 수 있는 자리라 남긴다 (#964).
                    errorReporter.recordMindRecordFailure(MindRecordFailureStage.MEDIA_UPLOAD, e)
                    // null 로 흡수하면 사용자는 이미지가 붙은 줄 알고 저장한다 (#716).
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            imageUploadError = UiText.Resource(R.string.mindrecord_error_image_upload_failed),
                        )
                    }
                }.getOrNull()
                ?.fileUrl
        }

        fun submit(isDraft: Boolean = false) {
            val state = _uiState.value
            // 임시저장과 정식 등록의 조건을 가른다. 무엇이 막고 있는지도 알린다 —
            // 종전에는 사유 없이 return 해 버튼이 고장 난 것처럼 보였다 (#722).
            if (isDraft) {
                val missing = state.missingForDraft()
                if (missing != null) {
                    failSubmit(missing)
                    return
                }
                if (!state.canSaveDraft) return
            } else {
                val missing = state.missingForSubmit()
                if (missing != null) {
                    failSubmit(missing)
                    return
                }
                if (!state.canSubmit) return
            }
            // 고르지 않은 기분을 지어내지 않는다. 지어내면 그것이 사용자 데이터가 되고
            // (이어쓰기로 열면 «그냥그래» 가 이미 선택돼 보인다) 주간리포트 집계와 감정
            // 분석 입력에도 그대로 들어간다. 위 가드가 미선택을 이미 막는다.
            val mood = state.mood ?: return

            viewModelScope.launch {
                _uiState.update { it.copy(submitState = SubmitState.InProgress) }
                val result =
                    if (editingDiaryId != null) {
                        repository.update(
                            id = editingDiaryId,
                            payload =
                                DiaryUpdatePayload(
                                    title = state.title,
                                    content = state.content.toWireContent(uploadedFileKeysByUrl.snapshot()),
                                    isDraft = isDraft,
                                    todayMood = mood,
                                    // 생성 경로와 같은 규칙. 빈 선택을 빈 목록으로 보내면 서버가
                                    // 전체 해제로 읽어, 수신자를 건드리지 않은 편집이 기존 지정을
                                    // 지운다 — 목록 응답에 수신자가 없어 되살릴 수도 없다.
                                    receiverIds = state.selectedReceiverIds.toList().takeIf { it.isNotEmpty() },
                                    // 화면 값이 서버에서 왔거나 사용자가 고른 것일 때만 싣는다.
                                    // 그 밖에는 키를 생략해 서버가 기존 기록일을 유지한다 (#1008).
                                    date = state.date.takeIf { state.isDateChosen },
                                ),
                        )
                    } else {
                        repository.create(
                            DiaryCreatePayload(
                                title = state.title,
                                content = state.content.toWireContent(uploadedFileKeysByUrl.snapshot()),
                                isDraft = isDraft,
                                todayMood = mood,
                                receiverIds = state.selectedReceiverIds.toList(),
                                date = state.date,
                            ),
                        )
                    }
                result
                    .onSuccess {
                        // 서버가 permanent 로 옮겼으니 이 URL 들은 더 이상 치환 대상이 아니다.
                        uploadedFileKeysByUrl.clear()
                        _uiState.update { it.copy(submitState = SubmitState.Succeeded) }
                        // 임시저장이 하나 늘었으니 툴바 숫자도 따라가야 한다 (#769).
                        if (isDraft) loadDraftCount()
                    }.onFailure { e ->
                        // 방금 쓴 글이 서버에 닿지 못한 자리다 — 재현이 어려워 실기 QA 로
                        // 잡히지 않으므로 텔레메트리에 남긴다 (#964).
                        errorReporter.recordMindRecordFailure(MindRecordFailureStage.DIARY_SUBMIT, e)
                        _uiState.update {
                            it.copy(
                                submitState =
                                    SubmitState.Failed(
                                        UiText.Resource(R.string.mindrecord_error_diary_submit_failed),
                                    ),
                            )
                        }
                    }
            }
        }

        fun consumeSubmitResult() {
            _uiState.update { it.copy(submitState = SubmitState.Idle) }
        }

        // 실패해도 수신자 행이 "수신자 설정하기" 로 남을 뿐 작성 자체는 가능 — 에러는 조용히 무시.
        private fun failSubmit(
            @StringRes messageRes: Int,
        ) {
            _uiState.update {
                it.copy(submitState = SubmitState.Failed(UiText.Resource(messageRes)))
            }
        }

        /**
         * 수신인 목록 조회. **실패를 빈 목록으로 흡수하지 않는다** (#1019).
         *
         * 흡수하면 화면이 «아직 등록 안 함» 과 «못 불러옴» 을 구분하지 못해, 사용자는 시트를
         * 열어 빈 목록만 본다. 수신자 없이도 작성은 되므로 화면을 막지는 않는다.
         */
        fun loadReceivers() {
            viewModelScope.launch {
                _uiState.update { it.copy(isReceiverLoading = true, receiverLoadError = null) }
                runCatchingCancellable { userRepository.getReceivers() }
                    .onSuccess { receivers ->
                        _uiState.update {
                            it.copy(receivers = receivers, isReceiverLoading = false, receiverLoadError = null)
                        }
                    }.onFailure {
                        // 서버·예외 원문을 화면 문구로 쓰지 않는다 — 오프라인이면 «Unable to
                        // resolve host …» 가 그대로 노출된다. 도메인 문구로 고정한다 (#614·#1019).
                        _uiState.update {
                            it.copy(
                                isReceiverLoading = false,
                                receiverLoadError = UiText.Resource(R.string.mindrecord_error_receiver_load_failed),
                            )
                        }
                    }
            }
        }

        // 단건 조회 엔드포인트가 없어 해당 달의 draft 목록에서 id 로 찾는다.

        /**
         * 수정 대상 일기를 프리필한다.
         *
         * 임시저장과 정식 기록은 **같은 목록 API 의 `draftOnly` 만 다르다.** 종전에는 임시저장
         * 경로만 있어서 목록의 "수정하기" 가 갈 곳이 없었다 (#582).
         */
        private fun loadExisting(
            diaryId: Long,
            yearMonth: String?,
            isDraft: Boolean,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isDraftLoading = true, draftLoadError = null) }
                repository
                    .getList(
                        yearMonth = yearMonth ?: YearMonth.now().toString(),
                        draftOnly = if (isDraft) true else null,
                    ).mapCatching { list -> list.diaries.first { it.diaryId == diaryId } }
                    .onSuccess { draft ->
                        _uiState.update {
                            it.copy(
                                title = draft.title,
                                content = draft.content,
                                mood = draft.todayMood,
                                // 서버가 준 기록일을 그대로 보여 주고, 그때부터 수정 요청에도 싣는다.
                                // 프리필이 날짜를 못 주면 화면 값(오늘)을 유지하되 `isDateChosen` 은
                                // false 로 남겨, 수정이 기존 기록일을 오늘로 밀지 않게 한다 (#1008).
                                date = draft.toUi()?.date ?: it.date,
                                isDateChosen = draft.toUi()?.date != null || it.isDateChosen,
                                isDraftLoading = false,
                                draftLoaded = true,
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isDraftLoading = false,
                                draftLoadError =
                                    UiText.Resource(R.string.mindrecord_error_diary_draft_load_failed),
                            )
                        }
                    }
            }
        }
    }
