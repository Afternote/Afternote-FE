package com.afternote.feature.mindrecord.presentation.viewmodel

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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DailyQuestionWriteUiState())
        val uiState: StateFlow<DailyQuestionWriteUiState> = _uiState.asStateFlow()

        /** 수정 대상 답변 ID. 목록의 "수정하기" 로 들어오면 채워진다 (#582). */
        private val editingAnswerId: Long? =
            savedStateHandle.toRoute<MindRecordRoute.DailyQuestionWriteRoute>().answerId

        init {
            if (editingAnswerId != null) loadAnswer(editingAnswerId) else loadTodayQuestion()
        }

        /**
         * 정식 답변을 프리필한다 (#582).
         *
         * 오늘 질문을 다시 묻지 않는다 — 수정 대상은 이미 특정된 레코드이고, 저장은
         * `PATCH /daily-questions/{id}` 로 나간다. `questionId` 도 그래서 필요 없다.
         */
        private fun loadAnswer(answerId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isQuestionLoading = true, questionLoadError = null) }
                repository
                    .getList()
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
                    answer = draft.content,
                    imageUrl = draft.imageUrl ?: it.imageUrl,
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

        fun submit(isDraft: Boolean = false) {
            val state = _uiState.value
            if (!state.canSubmit) return
            // 수정 모드에는 오늘 질문을 부르지 않으므로 questionId 가 없다. PATCH 는
            // 대상 레코드 ID 만 있으면 되고, 명세에도 questionId 가 없다 (#582).
            // canSubmit 이 이미 둘 중 하나는 있음을 보장한다.
            val questionId = state.questionId

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
                                questionId = requireNotNull(questionId),
                                imageUrl = state.imageUrl,
                            ),
                        )
                    }
                result
                    .onSuccess {
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
    }
