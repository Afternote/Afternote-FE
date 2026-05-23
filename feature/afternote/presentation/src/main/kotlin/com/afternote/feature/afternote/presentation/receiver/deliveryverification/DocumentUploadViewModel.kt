package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.error.ReceiverDeliverySubmitException
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
            _uiState.update {
                it.copy(isSubmitting = true, errorMessageRes = null, errorMessage = null)
            }
            viewModelScope.launch {
                receiverAuthRepository
                    .submitDeliveryVerification(deathUrl, famRelUrl)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _events.send(DocumentUploadEvent.Submitted)
                    }.onFailure { throwable ->
                        // 두 갈래로 분기한다:
                        //  (1) data 레이어가 ApiException 을 ReceiverDeliverySubmitException 으로 매핑해 내려준 경우
                        //      → 백엔드가 보낸 사용자 친화 message(예: 409 "이미 대기 중인 인증 요청이 존재합니다.")
                        //        가 serverMessage 에 담겨 있으므로 그대로 노출.
                        //  (2) 그 외 throwable (UnknownHostException, SerializationException 등 도메인 매핑 안 된 인프라 예외)
                        //      → 기술적 메시지라 사용자 노출 부적합. generic 한국어 문구로 fallback.
                        //
                        // `as?` = safe cast: 타입 일치 시 캐스팅 값, 불일치 시 null. (1)이면 값, (2)이면 null.
                        // 이어지는 `?.serverMessage?.takeIf { ... }` 는 null-safe 체인 + 빈 문자열도 null 로 폴백.
                        val serverMessage =
                            (throwable as? ReceiverDeliverySubmitException)?.serverMessage?.takeIf { it.isNotBlank() }
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = serverMessage,
                                errorMessageRes = R.string.receiver_verify_submit_failed.takeIf { serverMessage == null },
                            )
                        }
                    }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(errorMessageRes = null, errorMessage = null) }
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
