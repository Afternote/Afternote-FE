package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCreatePayload
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import com.afternote.feature.mindrecord.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
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
        private val repository: DeepThoughtRepository,
        private val photoUploadRepository: PhotoUploadRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DeepThoughtWriteUiState())
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
                                        UiText.DynamicOrResource(
                                            value = e.message,
                                            fallbackResId = R.string.mindrecord_error_deep_thought_submit_failed,
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
