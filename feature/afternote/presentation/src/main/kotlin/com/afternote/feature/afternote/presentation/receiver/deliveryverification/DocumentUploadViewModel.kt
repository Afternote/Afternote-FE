package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverAuthRepository
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverDeliveryDocumentUploadRepository
import com.afternote.feature.afternote.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 증빙 서류 업로드(6·7·8) ViewModel — 두 서류 슬롯 업로드 + 열람 신청 제출 (이슈 #215).
 *
 * 슬롯별 파일 바이트는 UI 의 picker 콜백이 ContentResolver 로 추출해 [uploadDocument] 로 전달한다 —
 * 도메인 레이어가 Android Uri 에 의존하지 않도록 분리. 업로드 성공 시 슬롯에 fileUrl 이 채워지고,
 * 양 슬롯 fileUrl 이 모두 채워지면 "다음" 활성 → [submit] 으로 `submitDeliveryVerification` 호출.
 */
@HiltViewModel
class DocumentUploadViewModel
    @Inject
    constructor(
        private val uploadRepository: ReceiverDeliveryDocumentUploadRepository,
        private val receiverAuthRepository: ReceiverAuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DocumentUploadUiState())
        val uiState: StateFlow<DocumentUploadUiState> = _uiState.asStateFlow()

        private val _events = Channel<DocumentUploadEvent>(Channel.BUFFERED)
        val events: Flow<DocumentUploadEvent> = _events.receiveAsFlow()

        fun uploadDocument(
            slot: DocumentSlot,
            bytes: ByteArray,
            extension: String,
            displayName: String,
        ) {
            if (bytes.isEmpty()) return
            updateSlot(slot) { it.copy(displayName = displayName, isUploading = true) }
            viewModelScope.launch {
                uploadRepository
                    .upload(bytes, extension)
                    .onSuccess { fileUrl ->
                        updateSlot(slot) {
                            it.copy(
                                fileUrl = fileUrl,
                                isUploading = false,
                            )
                        }
                    }.onFailure {
                        updateSlot(slot) { DocumentSlotState() }
                        _uiState.update {
                            it.copy(errorMessageRes = R.string.receiver_verify_document_upload_failed)
                        }
                    }
            }
        }

        fun submit() {
            val state = _uiState.value
            val deathUrl = state.deathCertificate.fileUrl
            val famRelUrl = state.familyRelationCertificate.fileUrl
            if (deathUrl == null || famRelUrl == null || state.isSubmitting) {
                _uiState.update {
                    it.copy(errorMessageRes = R.string.receiver_verify_documents_required)
                }
                return
            }
            _uiState.update { it.copy(isSubmitting = true, errorMessageRes = null) }
            viewModelScope.launch {
                receiverAuthRepository
                    .submitDeliveryVerification(deathUrl, famRelUrl)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _events.send(DocumentUploadEvent.Submitted)
                    }.onFailure {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessageRes = R.string.receiver_verify_submit_failed,
                            )
                        }
                    }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(errorMessageRes = null) }
        }

        private inline fun updateSlot(
            slot: DocumentSlot,
            transform: (DocumentSlotState) -> DocumentSlotState,
        ) {
            _uiState.update { state ->
                when (slot) {
                    DocumentSlot.DeathCertificate -> {
                        state.copy(deathCertificate = transform(state.deathCertificate))
                    }

                    DocumentSlot.FamilyRelationCertificate -> {
                        state.copy(familyRelationCertificate = transform(state.familyRelationCertificate))
                    }
                }
            }
        }
    }
