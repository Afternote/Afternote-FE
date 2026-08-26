package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.mindrecord.presentation.util.isHtmlBlank
import com.afternote.feature.mindrecord.presentation.util.toWireContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DailyQuestionWriteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: DailyQuestionRepository,
        private val photoUploadRepository: PhotoUploadRepository,
        private val draftLoader: MindRecordDraftLoader,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DailyQuestionWriteUiState())
        val uiState: StateFlow<DailyQuestionWriteUiState> = _uiState.asStateFlow()

        private val route = savedStateHandle.toRoute<MindRecordRoute.DailyQuestionWriteRoute>()

        /** 프리필 대상 레코드 ID. 목록의 "수정하기"(#582)·임시저장 목록 탭(#770)이 채운다. */
        private val editingAnswerId: Long? = route.answerId

        init {
            // 수정 진입이면 대상 레코드를 프리필하고, 신규면 오늘 질문을 부른다 (#582).
            // 임시저장은 draftOnly=true 로만 내려오므로 어느 목록을 볼지 isDraft 로 가른다 (#770).
            if (editingAnswerId != null) {
                loadAnswer(editingAnswerId, route.isDraft)
            } else {
                loadTodayQuestion()
            }
            loadDraftCount()
        }

        /**
         * 대상 레코드를 프리필한다 (#582·#770).
         *
         * 오늘 질문을 다시 묻지 않는다 — 대상은 이미 특정된 레코드이고, 저장은
         * `PATCH /daily-questions/{id}` 로 나간다. `questionId` 도 그래서 필요 없다.
         * 임시저장 이어쓰기든 정식 답변 수정이든 조회하는 목록의 `draftOnly` 만 다르다.
         */
        private fun loadAnswer(
            answerId: Long,
            isDraft: Boolean,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isQuestionLoading = true, questionLoadError = null) }
                repository
                    // 임시저장은 draftOnly=true 로만 내려온다. 당일이 지난 draft 는 이 경로가
                    // 유일한 진입 수단이다 (#770).
                    .getList(draftOnly = if (isDraft) true else null)
                    .mapCatching { list -> list.first { it.dailyQuestionId == answerId } }
                    .onSuccess { answer ->
                        _uiState.update {
                            it.copy(
                                draftId = answer.dailyQuestionId,
                                questionContent = answer.title,
                                answer = answer.content,
                                isQuestionLoading = false,
                                contentLoaded = true,
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isQuestionLoading = false,
                                questionLoadError =
                                    UiText.DynamicOrResource(
                                        value = e.message,
                                        fallbackResId = R.string.mindrecord_error_daily_question_today_failed,
                                    ),
                            )
                        }
                    }
            }
        }

        /** 툴바 카운트는 화면 장식이라 실패해도 화면을 막지 않고 '모름' 으로 남긴다. */
        private fun loadDraftCount() {
            viewModelScope.launch {
                draftLoader.count().onSuccess { count ->
                    _uiState.update { it.copy(draftCount = count) }
                }
            }
        }

        private fun loadTodayQuestion() {
            viewModelScope.launch {
                _uiState.update { it.copy(isQuestionLoading = true, questionLoadError = null) }
                repository
                    .getToday()
                    .onSuccess { today ->
                        _uiState.update {
                            it.copy(
                                questionId = today.questionId,
                                questionDay = today.day,
                                questionContent = today.content,
                                isQuestionLoading = false,
                            )
                        }
                        if (today.isDraft) resumeDraft()
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isQuestionLoading = false,
                                questionLoadError =
                                    UiText.Resource(R.string.mindrecord_error_daily_question_today_failed),
                            )
                        }
                    }
            }
        }

        /**
         * today 응답이 draft 존재를 알려주면 당일 임시저장 레코드를 찾아 이어쓰기 상태로 프리필한다.
         * 없으면 아무것도 하지 않고 신규 작성으로 폴백.
         *
         * 서버는 `draftOnly` 없이 조회하면 임시저장을 제외한 답변만 내려주므로 반드시 `draftOnly = true` 로 보낸다.
         * 파라미터를 무시하는 서버를 만나도 오답을 잡지 않도록 `isDraft` 재확인은 남겨둔다.
         *
         * 이어쓸 본문은 today 응답에 없어 이 목록 조회가 필요하다. 반면 `draftId` 확보만을 위한
         * 조회는 두지 않는다 — 서버가 같은 `questionId` 에 대해 upsert 라 `draftId` 가 null 인 채로
         * POST 가 다시 나가도 레코드는 갱신될 뿐 중복 생성되지 않는다.
         *
         * **이미 입력된 값은 덮지 않는다.** [submit] 이 `questionId` 부재를 만나면 조회를 다시
         * 걸기 때문에, 화면 진입뿐 아니라 사용자가 답변을 다 쓰고 이미지를 고른 뒤에도 이 경로가
         * 돈다. 무조건 덮으면 그 순간 방금 넣은 답변·이미지가 서버 임시저장본으로 교체된다.
         * 화면 값이 비어 있을 때만 draft 로 채우고, `draftId` 는 화면 입력과 겹치지 않아 항상 채운다
         * (없으면 재제출이 POST 로 나가 이어쓰기가 안 된다).
         */
        private suspend fun resumeDraft() {
            _uiState.update { it.copy(isResumingDraft = true) }
            val draft =
                repository
                    .getList(date = LocalDate.now().toString(), draftOnly = true)
                    .getOrNull()
                    ?.firstOrNull { it.isDraft }
                    ?: run {
                        _uiState.update { it.copy(isResumingDraft = false) }
                        return
                    }
            _uiState.update {
                // 빈 에디터는 `<p></p>` 를 내보내므로 `isBlank()` 로는 "비어 있음" 을 판정할 수
                // 없다. 태그를 걷어 낸 본문으로 판단해야 사용자가 아무것도 안 쓴 상태에서
                // 이어쓸 내용이 실린다 (#923).
                val appliesDraft = it.answer.isHtmlBlank()
                it.copy(
                    draftId = draft.dailyQuestionId,
                    answer = if (appliesDraft) draft.content else it.answer,
                    isResumingDraft = false,
                    // 화면이 이 전환을 보고 에디터를 재생성해 본문을 다시 싣는다 (#923).
                    // **다시 실을 값이 있을 때만** 뒤집는다 — 사용자가 이미 쓴 내용을 보존한
                    // 경우까지 재생성하면 커서·포커스·IME 조합만 리셋되고, 진행 중이던
                    // 업로드가 끊길 수 있다 (리뷰 지적).
                    draftLoaded = it.draftLoaded || appliesDraft,
                )
            }
        }

        /** 이번 작성 중 업로드한 이미지의 원본 URL. 제출 시 fileKey 로 바꿀 대상이다 (#549). */
        private val uploadedImageUrls = mutableSetOf<String>()

        fun onAnswerChanged(text: String) {
            _uiState.update { it.copy(answer = text) }
        }

        /**
         * 에디터에서 고른 **미디어**(사진·음성·파일)를 업로드하고 **미리보기에 쓸 URL** 을 반환한다 (실패 시 null).
         *
         * 반환한 URL 은 에디터가 본문 HTML 에 `<img src>` 로 삽입한다 — 그것이 서버에
         * 이미지가 남는 유일한 경로다. 종전에는 이 URL 을 등록 payload 의 `imageUrl` 로도
         * 실어 보냈지만, 그 필드는 계약에 없어 서버가 통째로 무시했다 (#549).
         *
         * 다만 **저장 시 나가는 값은 이 URL 이 아니다.** 서버는 본문의 `img src` 에서 fileKey
         * 를 기대하므로, 여기서 받은 URL 을 [uploadedImageUrls] 에 기억해 뒀다가 제출 직전에
         * 키 형태로 바꾼다 ([toWireContent]). 미리보기는 전체 URL 이라야 뜬다.
         */
        suspend fun uploadMedia(uriString: String): String? {
            // 실패 문구의 수명은 «다음 업로드 시작까지» 다 — 화면에 걷는 수단이 따로 없고,
            // 걷는 함수만 두면 호출부 0건인 죽은 코드가 된다 (#1019 리뷰 지적).
            _uiState.update { it.copy(isUploadingImage = true, imageUploadError = null) }
            return photoUploadRepository
                .upload(uriString = uriString, directory = MIND_RECORD_UPLOAD_DIRECTORY)
                .onSuccess { url ->
                    // 계약에 imageUrl 이 없어 상태로 들지 않는다 — 본문 img 로 들어가고,
                    // 제출 직전 fileKey 로 바뀔 수 있게 기억만 해 둔다 (#549).
                    uploadedImageUrls += url
                    _uiState.update { it.copy(isUploadingImage = false) }
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

        /**
         * 저장(`isDraft=false`) / 임시저장(`isDraft=true`).
         *
         * 중단할 때는 반드시 사유를 [SubmitState.Failed] 로 남긴다. 종전에는 `questionId` 가
         * null 이면 조용히 return 해, 오늘 질문 조회가 실패한 상태에서 저장·임시저장 둘 다
         * 요청 0건·화면 무변화로 고장처럼 보였다 (#565).
         */
        fun submit(isDraft: Boolean = false) {
            val state = _uiState.value
            if (state.submitState == SubmitState.InProgress) return

            // 하단 툴바 임시저장은 `enabled` 없는 clickable 이라 canSubmit 을 우회한다.
            // 차단은 화면이 아니라 여기서 지킨다.
            if (state.isResumingDraft) return
            // 업로드가 끝나기 전에 나가면 이미지 없는 기록이 저장된다 — 일기 화면은
            // submit() 초입의 canSubmit 가드로 이미 막고 있다 (리뷰 지적).
            if (state.isUploadingImage) {
                failSubmit(R.string.mindrecord_error_image_uploading)
                return
            }

            if (state.answer.isBlank()) {
                failSubmit(R.string.mindrecord_error_daily_question_answer_required)
                return
            }

            // 수정·이어쓰기는 오늘 질문을 부르지 않아 questionId 가 없다. PATCH 는 대상 레코드
            // ID 만 있으면 되고 명세에도 questionId 가 없다 (#582). 그래서 «둘 다 없을 때» 만
            // 막는다 — 신규 작성인데 오늘 질문 조회가 실패한 경우다.
            val questionId = state.questionId
            // 수정·이어쓰기 대상 레코드가 있으면 PATCH 로 나가므로 questionId 가 없어도 된다 (#582·#770).
            if (questionId == null && state.draftId == null) {
                // 서버가 답변을 어디에 붙일지 알 수 없어 요청 자체가 불가능하다. 사유를
                // 알리고 조회를 다시 걸어 사용자가 재시도할 수 있게 한다 (#565).
                failSubmit(R.string.mindrecord_error_daily_question_missing)
                // 이미 조회 중이면 그대로 둔다 — 연타로 같은 요청을 겹쳐 쌓지 않는다.
                if (!state.isQuestionLoading) loadTodayQuestion()
                return
            }
            if (!state.canSubmit) return

            viewModelScope.launch {
                _uiState.update { it.copy(submitState = SubmitState.InProgress) }
                val result =
                    if (state.draftId != null) {
                        // 기존 임시저장 레코드가 있으면 새로 만들지 않고 PATCH 로 갱신/전환한다.
                        repository.update(
                            id = state.draftId,
                            payload =
                                DailyQuestionUpdatePayload(
                                    content = state.answer.toWireContent(uploadedImageUrls),
                                    isDraft = isDraft,
                                    questionId = questionId,
                                ),
                        )
                    } else {
                        repository.create(
                            DailyQuestionCreatePayload(
                                content = state.answer.toWireContent(uploadedImageUrls),
                                isDraft = isDraft,
                                // 생성 경로는 questionId 가 반드시 있다 — 위 가드가 null 을 걸렀다.
                                questionId = requireNotNull(questionId),
                            ),
                        )
                    }
                result
                    .onSuccess { savedId ->
                        _uiState.update {
                            it.copy(
                                submitState = SubmitState.Succeeded,
                                // 서버가 돌려준 "내 답변" 식별자를 그대로 든다 (#573).
                                // 임시저장 뒤 이어서 저장하면 목록을 다시 뒤지지 않고 이 값으로 PATCH 한다 —
                                // 종전에는 응답을 버려 `resumeDraft()` 가 목록을 재조회해 첫 draft 를
                                // 추측으로 골랐다.
                                draftId = if (isDraft) savedId else null,
                            )
                        }
                        // 임시저장이 하나 늘었으니 툴바 숫자도 따라가야 한다 (#769).
                        if (isDraft) loadDraftCount()
                        // 제출이 성공하면 staging 키는 이미 permanent 로 옮겨졌다. 남겨 두면
                        // 같은 ViewModel 로 두 번 제출될 때(예: 임시저장 뒤 화면에 머무는 흐름)
                        // 이미 옮겨진 파일을 다시 옮기려다 실패한다 (#549).
                        uploadedImageUrls.clear()
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                submitState =
                                    SubmitState.Failed(
                                        UiText.Resource(R.string.mindrecord_error_daily_question_submit_failed),
                                    ),
                            )
                        }
                    }
            }
        }

        fun consumeSubmitResult() {
            _uiState.update { it.copy(submitState = SubmitState.Idle) }
        }

        private fun failSubmit(
            @StringRes messageResId: Int,
        ) {
            _uiState.update {
                it.copy(submitState = SubmitState.Failed(UiText.Resource(messageResId)))
            }
        }
    }
