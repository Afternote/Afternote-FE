package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyQuestionWriteViewModel
    @Inject
    constructor(
        private val repository: DailyQuestionRepository,
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
                                questionContent = today.content,
                                isQuestionLoading = false,
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isQuestionLoading = false,
                                questionLoadError = e.message ?: "오늘의 질문을 불러오지 못했습니다.",
                            )
                        }
                    }
            }
        }

        fun onAnswerChanged(text: String) {
            _uiState.update { it.copy(answer = text) }
        }

        fun submit(isDraft: Boolean = false) {
            val state = _uiState.value
            val questionId = state.questionId ?: return
            if (!state.canSubmit) return

            viewModelScope.launch {
                _uiState.update { it.copy(submitState = SubmitState.InProgress) }
                repository
                    .create(
                        DailyQuestionCreatePayload(
                            content = state.answer,
                            isDraft = isDraft,
                            questionId = questionId,
                            imageUrl = state.imageUrl,
                        ),
                    ).onSuccess {
                        _uiState.update { it.copy(submitState = SubmitState.Succeeded) }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(submitState = SubmitState.Failed(e.message ?: "등록에 실패했습니다."))
                        }
                    }
            }
        }

        fun consumeSubmitResult() {
            _uiState.update { it.copy(submitState = SubmitState.Idle) }
        }
    }
