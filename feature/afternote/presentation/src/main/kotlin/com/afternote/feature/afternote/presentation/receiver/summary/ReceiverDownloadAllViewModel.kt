package com.afternote.feature.afternote.presentation.receiver.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.repository.ReceiverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 모든 기록 내려받기: 인증번호로 묶음을 조회한 뒤 파일로 저장합니다.
 */
@HiltViewModel
class ReceiverDownloadAllViewModel
    @Inject
    constructor(
        private val receiverRepository: ReceiverRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReceiverDownloadAllUiState())
        val uiState: StateFlow<ReceiverDownloadAllUiState> = _uiState.asStateFlow()

        fun onEvent(event: ReceiverDownloadAllEvent) {
            when (event) {
                is ReceiverDownloadAllEvent.ConfirmDownload -> handleConfirmDownload(event.authCode)
                is ReceiverDownloadAllEvent.DownloadSuccessConsumed -> handleClearDownloadSuccess()
                is ReceiverDownloadAllEvent.ErrorConsumed -> handleClearError()
            }
        }

        private fun handleConfirmDownload(authCode: String) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(isLoading = true, errorMessage = null, downloadSuccess = false)
                }
                receiverRepository
                    .downloadAllReceived(authCode)
                    .onSuccess { result ->
                        receiverRepository
                            .saveReceivedExportToFile(result)
                            .onSuccess {
                                _uiState.update {
                                    it.copy(isLoading = false, downloadSuccess = true)
                                }
                            }.onFailure { e ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessage = e.message ?: "파일 저장에 실패했습니다.",
                                    )
                                }
                            }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "모든 기록 내려받기에 실패했습니다.",
                            )
                        }
                    }
            }
        }

        private fun handleClearDownloadSuccess() {
            _uiState.update { it.copy(downloadSuccess = false) }
        }

        private fun handleClearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }
    }
