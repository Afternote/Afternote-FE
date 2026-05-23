package com.afternote.feature.mindrecord.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
        @ApplicationContext private val context: Context,
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
                            it.copy(
                                submitState =
                                    SubmitState.Failed(
                                        e.message ?: context.getString(R.string.mindrecord_error_diary_submit_failed),
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
