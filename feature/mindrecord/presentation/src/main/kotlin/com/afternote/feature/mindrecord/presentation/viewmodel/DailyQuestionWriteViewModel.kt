package com.afternote.feature.mindrecord.presentation.viewmodel

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
                        if (today.isDraft) resumeDraftIfFound()
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
         * today 응답이 draft 존재를 알려주면 당일 목록에서 임시저장 레코드를 찾아 이어쓰기 상태로 프리필한다.
         * 목록에 draft 항목/플래그가 없으면 (서버 계약 미검증) 아무것도 하지 않고 신규 작성으로 폴백.
         */
        private suspend fun resumeDraftIfFound() {
            val draft =
                repository
                    .getList(date = LocalDate.now().toString())
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
            val questionId = state.questionId ?: return
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
