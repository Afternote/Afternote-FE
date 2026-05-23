package com.afternote.feature.mindrecord.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCreatePayload
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import com.afternote.feature.mindrecord.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeepThoughtWriteViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val repository: DeepThoughtRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                DeepThoughtWriteUiState(
                    category = context.getString(R.string.mindrecord_deep_thought_write_default_category),
                ),
            )
        val uiState: StateFlow<DeepThoughtWriteUiState> = _uiState.asStateFlow()

        fun onTitleChanged(value: String) {
            _uiState.update { it.copy(title = value) }
        }

        fun onContentChanged(value: String) {
            _uiState.update { it.copy(content = value) }
        }

        fun onCategoryChanged(value: String) {
            _uiState.update { it.copy(category = value) }
        }

        fun onTagsChanged(tags: List<String>) {
            _uiState.update { it.copy(tags = tags) }
        }

        fun submit(isDraft: Boolean = false) {
            val state = _uiState.value
            if (!state.canSubmit) return

            viewModelScope.launch {
                _uiState.update { it.copy(submitState = SubmitState.InProgress) }
                repository
                    .create(
                        DeepThoughtCreatePayload(
                            title = state.title,
                            content = state.content,
                            isDraft = isDraft,
                            category = state.category,
                            tags = state.tags.takeIf { it.isNotEmpty() },
                            imageUrl = state.imageUrl,
                        ),
                    ).onSuccess {
                        _uiState.update { it.copy(submitState = SubmitState.Succeeded) }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                submitState =
                                    SubmitState.Failed(
                                        e.message ?: context.getString(R.string.mindrecord_error_deep_thought_submit_failed),
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
