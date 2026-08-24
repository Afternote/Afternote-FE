package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.util.toUploadedFileKey
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
        private val repository: DailyQuestionRepository,
        private val photoUploadRepository: PhotoUploadRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DailyQuestionWriteUiState())
        val uiState: StateFlow<DailyQuestionWriteUiState> = _uiState.asStateFlow()

        init {
            loadTodayQuestion()
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
                                    UiText.DynamicOrResource(
                                        value = e.message,
                                        fallbackResId = R.string.mindrecord_error_daily_question_today_failed,
                                    ),
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
            val draft =
                repository
                    .getList(date = LocalDate.now().toString(), draftOnly = true)
                    .getOrNull()
                    ?.firstOrNull { it.isDraft }
                    ?: return
            _uiState.update {
                it.copy(
                    draftId = draft.dailyQuestionId,
                    answer = if (it.answer.isBlank()) draft.content else it.answer,
                )
            }
        }

        /** 이번 작성 중 업로드한 이미지의 원본 URL. 제출 시 fileKey 로 바꿀 대상이다 (#549). */
        private val uploadedImageUrls = mutableSetOf<String>()

        fun onAnswerChanged(text: String) {
            _uiState.update { it.copy(answer = text) }
        }

        /**
         * 에디터에서 고른 이미지를 업로드하고 **미리보기에 쓸 URL** 을 반환한다 (실패 시 null).
         *
         * 반환한 URL 은 에디터가 본문 HTML 에 `<img src>` 로 삽입한다 — 그것이 서버에
         * 이미지가 남는 유일한 경로다. 종전에는 이 URL 을 등록 payload 의 `imageUrl` 로도
         * 실어 보냈지만, 그 필드는 계약에 없어 서버가 통째로 무시했다 (#549).
         *
         * 다만 **저장 시 나가는 값은 이 URL 이 아니다.** 서버는 본문의 `img src` 에서 fileKey
         * 를 기대하므로, 여기서 받은 URL 을 [uploadedImageUrls] 에 기억해 뒀다가 제출 직전에
         * 키 형태로 바꾼다 ([toWireContent]). 미리보기는 전체 URL 이라야 뜬다.
         */
        suspend fun uploadImage(uriString: String): String? =
            photoUploadRepository
                .upload(uriString = uriString, directory = MIND_RECORD_UPLOAD_DIRECTORY)
                .onSuccess { url -> uploadedImageUrls += url }
                .getOrNull()

        /**
         * 제출 직전, **이번 작성 중 업로드한** 이미지의 `src` 만 fileKey 로 바꾼다.
         *
         * 이미 저장돼 본문에 들어 있는 영구 URL 은 건드리지 않는다 — 서버가 그대로 통과시키고
         * (실측 확인), 키로 바꾸면 이미 옮겨진 파일을 다시 옮기려다 실패한다. 그래서 경로
         * 패턴으로 훑지 않고 이번에 받은 URL 만 정확히 치환한다.
         */
        private fun String.toWireContent(): String =
            uploadedImageUrls.fold(this) { content, url -> content.replace(url, url.toUploadedFileKey()) }

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

            if (state.answer.isBlank()) {
                failSubmit(R.string.mindrecord_error_daily_question_answer_required)
                return
            }

            val questionId = state.questionId
            if (questionId == null) {
                // 오늘 질문이 없으면 서버가 답변을 어디에 붙일지 알 수 없어 요청 자체가 불가능하다.
                // 사유를 알리고 조회를 다시 걸어 사용자가 재시도할 수 있게 한다.
                failSubmit(R.string.mindrecord_error_daily_question_missing)
                // 이미 조회 중이면 그대로 둔다 — 연타로 같은 요청을 겹쳐 쌓지 않는다.
                if (!state.isQuestionLoading) loadTodayQuestion()
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(submitState = SubmitState.InProgress) }
                val result =
                    if (state.draftId != null) {
                        // 기존 임시저장 레코드가 있으면 새로 만들지 않고 PATCH 로 갱신/전환한다.
                        repository.update(
                            id = state.draftId,
                            payload =
                                DailyQuestionUpdatePayload(
                                    content = state.answer.toWireContent(),
                                    isDraft = isDraft,
                                    questionId = questionId,
                                ),
                        )
                    } else {
                        repository.create(
                            DailyQuestionCreatePayload(
                                content = state.answer.toWireContent(),
                                isDraft = isDraft,
                                questionId = questionId,
                            ),
                        )
                    }
                result
                    .onSuccess {
                        // 제출이 성공하면 staging 키는 이미 permanent 로 옮겨졌다. 남겨 두면
                        // 같은 ViewModel 로 두 번 제출될 때(예: 임시저장 뒤 화면에 머무는 흐름)
                        // 이미 옮겨진 파일을 다시 옮기려다 실패한다. 그 전제를 화면 구조가
                        // 아니라 여기서 지킨다.
                        uploadedImageUrls.clear()
                        _uiState.update { it.copy(submitState = SubmitState.Succeeded) }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                submitState =
                                    SubmitState.Failed(
                                        UiText.DynamicOrResource(
                                            value = e.message,
                                            fallbackResId = R.string.mindrecord_error_daily_question_submit_failed,
                                        ),
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
