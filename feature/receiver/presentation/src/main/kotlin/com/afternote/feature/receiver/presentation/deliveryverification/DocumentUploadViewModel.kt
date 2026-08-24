package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.afternote.presentation.reporting.shouldReportInReceiverFlow
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverDeliveryDocumentUploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 증빙 서류 업로드(6·7·8) ViewModel — 두 서류 슬롯 업로드 + 열람 신청 제출 (이슈 #215).
 *
 * 슬롯별 파일 바이트는 UI 의 picker 콜백이 ContentResolver 로 추출해 [uploadDocument] 로 전달한다 —
 * 도메인 레이어가 Android Uri 에 의존하지 않도록 분리. 업로드 성공 시 슬롯에 fileUrl 이 채워지고,
 * 두 슬롯 중 하나 이상 fileUrl 이 채워지면 "다음" 활성 → [submit] 으로 `submitDeliveryVerification` 호출
 * (사망진단서/가족관계증명서 중 하나만으로 신청 가능 — 이슈 #380).
 */
@HiltViewModel
class DocumentUploadViewModel
    @Inject
    constructor(
        private val uploadRepository: ReceiverDeliveryDocumentUploadRepository,
        private val receiverAuthRepository: ReceiverAuthRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DocumentUploadUiState())
        val uiState: StateFlow<DocumentUploadUiState> = _uiState.asStateFlow()

        fun uploadDocument(
            slot: DocumentSlot,
            bytes: ByteArray,
            extension: String,
            displayName: String,
        ) {
            if (bytes.isEmpty()) {
                onDocumentReadFailed()
                return
            }
            // 실패 시 이 상태로 복원 — 재첨부 실패가 이미 성공해 둔 첨부(fileUrl)까지 지우면 안 된다 (#740).
            val previous = _uiState.value.slotOf(slot)
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
                    }.onFailure { throwable ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.DOCUMENT_UPLOAD, throwable)
                        updateSlot(slot) { previous }
                        _uiState.update {
                            it.copy(error = ErrorPayload.Res(R.string.receiver_verify_document_upload_failed))
                        }
                    }
            }
        }

        /** picker 가 돌려준 Uri 에서 바이트 추출이 실패한 경우 — 업로드 요청 전이므로 슬롯은 건드리지 않는다 (#740). */
        fun onDocumentReadFailed() {
            _uiState.update { it.copy(error = ErrorPayload.Res(R.string.receiver_verify_document_read_failed)) }
        }

        fun submit() {
            val state = _uiState.value
            // 버튼 비활성(canSubmit)과 별개의 최종 방어선 — 탭 시점과 recomposition 사이 race 로
            // 업로드 중에도 도달할 수 있고, 그대로 보내면 진행 중 파일이 신청에서 빠진다 (#711).
            if (state.deathCertificate.isUploading || state.familyRelationCertificate.isUploading) {
                _uiState.update {
                    it.copy(error = ErrorPayload.Res(R.string.receiver_verify_document_upload_in_progress))
                }
                return
            }
            val deathUrl = state.deathCertificate.fileUrl
            val famRelUrl = state.familyRelationCertificate.fileUrl
            if ((deathUrl == null && famRelUrl == null) || state.isSubmitting) {
                _uiState.update {
                    it.copy(error = ErrorPayload.Res(R.string.receiver_verify_documents_required))
                }
                return
            }
            _uiState.update {
                it.copy(isSubmitting = true, error = null)
            }
            viewModelScope.launch {
                receiverAuthRepository
                    .submitDeliveryVerification(deathUrl, famRelUrl)
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                    }.onFailure { throwable ->
                        // 서버가 사유 문구를 준 거절(이미 대기 중 등)은 예상된 경로라 리포팅하지 않는다.
                        if (throwable.shouldReportInReceiverFlow()) {
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.DELIVERY_SUBMIT, throwable)
                        }
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                error = throwable.toErrorPayload(R.string.receiver_verify_submit_failed),
                            )
                        }
                    }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(error = null) }
        }

        fun onSubmittedConsumed() {
            _uiState.update { it.copy(isSubmitted = false) }
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

        private fun DocumentUploadUiState.slotOf(slot: DocumentSlot): DocumentSlotState =
            when (slot) {
                DocumentSlot.DeathCertificate -> deathCertificate
                DocumentSlot.FamilyRelationCertificate -> familyRelationCertificate
            }
    }
