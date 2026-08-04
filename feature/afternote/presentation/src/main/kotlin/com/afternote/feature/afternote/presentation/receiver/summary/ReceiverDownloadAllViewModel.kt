package com.afternote.feature.afternote.presentation.receiver.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
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
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReceiverDownloadAllUiState())
        val uiState: StateFlow<ReceiverDownloadAllUiState> = _uiState.asStateFlow()

        fun onEvent(event: ReceiverDownloadAllEvent) {
            when (event) {
                ReceiverDownloadAllEvent.ConfirmDownload -> handleConfirmDownload()
                ReceiverDownloadAllEvent.DownloadSuccessConsumed -> handleClearDownloadSuccess()
                ReceiverDownloadAllEvent.ErrorConsumed -> handleClearError()
            }
        }

        private fun handleConfirmDownload() {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(isLoading = true, errorMessageRes = null, downloadSuccess = false)
                }
                receiverRepository
                    .downloadReceivedExport()
                    .onSuccess { result ->
                        receiverRepository
                            .saveReceivedExportToFile(result)
                            .onSuccess {
                                _uiState.update {
                                    it.copy(isLoading = false, downloadSuccess = true)
                                }
                            }.onFailure { e ->
                                errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVED_EXPORT_SAVE, e)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        errorMessageRes = R.string.receiver_download_all_save_failed,
                                    )
                                }
                            }
                    }.onFailure { e ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVED_EXPORT_DOWNLOAD, e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessageRes = R.string.receiver_download_all_failed,
                            )
                        }
                    }
            }
        }

        private fun handleClearDownloadSuccess() {
            _uiState.update { it.copy(downloadSuccess = false) }
        }

        private fun handleClearError() {
            _uiState.update { it.copy(errorMessageRes = null) }
        }
    }
