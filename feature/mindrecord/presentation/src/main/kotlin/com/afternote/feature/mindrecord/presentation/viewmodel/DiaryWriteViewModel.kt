package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DiaryWriteViewModel
    @Inject
    constructor(
        private val repository: DiaryRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DiaryWriteUiState())
        val uiState: StateFlow<DiaryWriteUiState> = _uiState.asStateFlow()

        fun onTitleChanged(value: String) {
            _uiState.update { it.copy(title = value) }
        }

        fun onContentChanged(value: String) {
            _uiState.update { it.copy(content = value) }
        }

        fun onMoodSelected(mood: TodayMood) {
            _uiState.update { it.copy(mood = mood) }
        }

        fun onDateSelected(date: LocalDate) {
            _uiState.update { it.copy(date = date) }
        }

        fun submit(isDraft: Boolean = false) {
            val state = _uiState.value
            if (!state.canSubmit) return

            viewModelScope.launch {
                _uiState.update { it.copy(submitState = SubmitState.InProgress) }
                repository
                    .create(
                        DiaryCreatePayload(
                            title = state.title,
                            content = state.content,
                            isDraft = isDraft,
                            todayMood = state.mood,
                            imageUrl = state.imageUrl,
                        ),
                    ).onSuccess {
                        _uiState.update { it.copy(submitState = SubmitState.Succeeded) }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(submitState = SubmitState.Failed(e.message ?: "일기 등록에 실패했습니다."))
                        }
                    }
            }
        }

        fun consumeSubmitResult() {
            _uiState.update { it.copy(submitState = SubmitState.Idle) }
        }
    }
