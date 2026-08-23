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
        private val draftLoader: MindRecordDraftLoader,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DailyQuestionWriteUiState())
        val uiState: StateFlow<DailyQuestionWriteUiState> = _uiState.asStateFlow()

        init {
            loadTodayQuestion()
            loadDraftCount()
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
                    imageUrl = it.imageUrl ?: draft.imageUrl,
                )
            }
        }

        fun onAnswerChanged(text: String) {
            _uiState.update { it.copy(answer = text) }
        }

        /**
         * 에디터에서 고른 이미지를 presigned URL 로 업로드하고 영구 URL 을 반환한다 (실패 시 null).
         * 첫 업로드 이미지는 등록 payload 의 `imageUrl` (목록 카드 썸네일) 로도 쓴다.
         */
        suspend fun uploadImage(uriString: String): String? =
            photoUploadRepository
                .upload(uriString = uriString, directory = MIND_RECORD_UPLOAD_DIRECTORY)
                .onSuccess { url ->
                    _uiState.update { if (it.imageUrl == null) it.copy(imageUrl = url) else it }
                }.getOrNull()

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
                                    content = state.answer,
                                    isDraft = isDraft,
                                    questionId = questionId,
                                    imageUrl = state.imageUrl,
                                ),
                        )
                    } else {
                        repository.create(
                            DailyQuestionCreatePayload(
                                content = state.answer,
                                isDraft = isDraft,
                                questionId = questionId,
                                imageUrl = state.imageUrl,
                            ),
                        )
                    }
                result
                    .onSuccess {
                        _uiState.update { it.copy(submitState = SubmitState.Succeeded) }
                        // 임시저장이 하나 늘었으니 툴바 숫자도 따라가야 한다 (#769).
                        if (isDraft) loadDraftCount()
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
