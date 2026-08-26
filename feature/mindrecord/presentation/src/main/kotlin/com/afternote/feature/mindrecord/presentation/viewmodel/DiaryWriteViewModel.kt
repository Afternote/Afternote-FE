package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
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
        private val userRepository: UserRepository,
        private val draftLoader: MindRecordDraftLoader,
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

        /** 이번 작성 중 업로드한 이미지의 원본 URL. 제출 시 fileKey 로 바꿀 대상이다 (#1016). */
        private val uploadedImageUrls = mutableSetOf<String>()

        /**
         * 에디터에서 고른 이미지를 presigned URL 로 업로드하고 **미리보기에 쓸 URL** 을 반환한다
         * (실패 시 null). 첫 업로드 이미지는 등록 payload 의 `imageUrl` (목록 카드 썸네일) 로도 쓴다.
         *
         * 저장 시 본문에 나가는 값은 이 URL 이 아니다 — 서버는 `img src` 에서 fileKey 를 기대하고
         * 전체 URL 을 받으면 호스트를 한 번 더 붙여 403 이 된다. 그래서 받은 URL 을 기억해 두고
         * 제출 직전에 키로 바꾼다 ([toWireContent]). 미리보기는 전체 URL 이라야 뜬다 (#1016).
         */
        suspend fun uploadImage(uriString: String): String? {
            _uiState.update { it.copy(isUploadingImage = true, imageUploadError = null) }
            return photoUploadRepository
                .upload(uriString = uriString, directory = MIND_RECORD_UPLOAD_DIRECTORY)
                .onSuccess { url ->
                    // 제출 직전 fileKey 로 바꿀 대상이다 (#1016).
                    uploadedImageUrls += url
                    _uiState.update {
                        val withUrl = if (it.imageUrl == null) it.copy(imageUrl = url) else it
                        withUrl.copy(isUploadingImage = false)
                    }
                }.onFailure {
                    // null 로 흡수하면 사용자는 이미지가 붙은 줄 알고 저장한다 (#716).
                    _uiState.update {
                        it.copy(
                            isUploadingImage = false,
                            imageUploadError = UiText.Resource(R.string.mindrecord_error_image_upload_failed),
                        )
                    }
                }.getOrNull()
        }

        fun consumeImageUploadError() {
            _uiState.update { it.copy(imageUploadError = null) }
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
                                    content = state.content.toWireContent(uploadedImageUrls),
                                    isDraft = isDraft,
                                    todayMood = mood,
                                    // 생성 경로와 같은 규칙. 빈 선택을 빈 목록으로 보내면 서버가
                                    // 전체 해제로 읽어, 수신자를 건드리지 않은 편집이 기존 지정을
                                    // 지운다 — 목록 응답에 수신자가 없어 되살릴 수도 없다.
                                    receiverIds = state.selectedReceiverIds.toList().takeIf { it.isNotEmpty() },
                                ),
                        )
                    } else {
                        repository.create(
                            DiaryCreatePayload(
                                title = state.title,
                                content = state.content.toWireContent(uploadedImageUrls),
                                isDraft = isDraft,
                                todayMood = mood,
                                receiverIds = state.selectedReceiverIds.toList(),
                            ),
                        )
                    }
                result
                    .onSuccess {
                        // 서버가 permanent 로 옮겼으니 이 URL 들은 더 이상 치환 대상이 아니다.
                        uploadedImageUrls.clear()
                        _uiState.update { it.copy(submitState = SubmitState.Succeeded) }
                        // 임시저장이 하나 늘었으니 툴바 숫자도 따라가야 한다 (#769).
                        if (isDraft) loadDraftCount()
                    }.onFailure { e ->
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

        private fun loadReceivers() {
            viewModelScope.launch {
                runCatching { userRepository.getReceivers() }
                    .onSuccess { receivers ->
                        _uiState.update { it.copy(receivers = receivers) }
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
                                // 표시 전용 값이다 — 서버가 날짜를 주면 그걸 보여 주고, 안 주면
                                // 화면에 이미 떠 있던 값을 유지한다. 고르는 수단은 없다 (#1008).
                                date = draft.toUi()?.date ?: it.date,
                                imageUrl = draft.imageUrl,
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
