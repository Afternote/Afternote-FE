package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.afternote.presentation.reporting.shouldReportInReceiverFlow
import com.afternote.feature.receiver.domain.error.DeliveryDocumentsMissingException
import com.afternote.feature.receiver.domain.repository.ReceiverDeliveryDocumentUploadRepository
import com.afternote.feature.receiver.domain.usecase.SubmitDeliveryVerificationUseCase
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.toReceiverErrorPopupOrNull
import com.afternote.feature.receiver.presentation.error.toReceiverErrorUiText
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
 * 두 슬롯 중 하나 이상 fileUrl 이 채워지면 "다음" 활성 → [submit] 이 [SubmitDeliveryVerificationUseCase] 호출.
 *
 * «서류가 최소 1장» 이라는 제출 불변식(#380)은 UseCase 가 소유한다 — 이 ViewModel 은 그 실패를
 * 화면 문구로 옮기기만 한다 (#1701). 업로드 진행 중 잠금(#711)과 중복 탭 차단은 화면 사정이라
 * 여기 남는다.
 */
@HiltViewModel
class DocumentUploadViewModel
    @Inject
    constructor(
        private val uploadRepository: ReceiverDeliveryDocumentUploadRepository,
        private val submitDeliveryVerification: SubmitDeliveryVerificationUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DocumentUploadUiState())
        val uiState: StateFlow<DocumentUploadUiState> = _uiState.asStateFlow()

        /**
         * 팝업의 "다시 시도하기" 가 되돌릴 마지막 시도 (#446). 팝업이 사라질 때 함께 비운다 —
         * 업로드 재시도는 사용자가 고른 파일 바이트를 붙들고 있어야 하므로, 안 비우면 화면이 살아
         * 있는 내내 서류 한 장이 힙에 남는다. 팝업 없이는 재시도 진입점 자체가 없으므로 팝업의
         * 수명이 곧 이 값의 수명이다.
         */
        private var pendingRetry: (() -> Unit)? = null

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
                        // 재시도는 같은 바이트를 다시 올리는 것이다 — 파일 선택부터 다시 시키면
                        // 「다시 시도하기」 가 이름과 달리 처음부터 하기가 된다 (#446).
                        showFailure(throwable, R.string.receiver_verify_document_upload_failed, uploadPath = true) {
                            uploadDocument(slot, bytes, extension, displayName)
                        }
                    }
            }
        }

        /** picker 가 돌려준 Uri 에서 바이트 추출이 실패한 경우 — 업로드 요청 전이므로 슬롯은 건드리지 않는다 (#740). */
        fun onDocumentReadFailed() {
            _uiState.update { it.copy(errorMessage = UiText.Resource(R.string.receiver_verify_document_read_failed)) }
        }

        fun submit() {
            val state = _uiState.value
            // 버튼 비활성(canSubmit)과 별개의 최종 방어선 — 탭 시점과 recomposition 사이 race 로
            // 업로드 중에도 도달할 수 있고, 그대로 보내면 진행 중 파일이 신청에서 빠진다 (#711).
            if (state.deathCertificate.isUploading || state.familyRelationCertificate.isUploading) {
                _uiState.update {
                    it.copy(errorMessage = UiText.Resource(R.string.receiver_verify_document_upload_in_progress))
                }
                return
            }
            // 이미 보낸 신청이 응답을 기다리는 중이면 두 번째 탭은 버린다 — 화면 사정이라 UseCase 로 내리지 않는다.
            if (state.isSubmitting) {
                _uiState.update {
                    it.copy(errorMessage = UiText.Resource(R.string.receiver_verify_documents_required))
                }
                return
            }
            _uiState.update {
                it.copy(isSubmitting = true, errorMessage = null)
            }
            viewModelScope.launch {
                submitDeliveryVerification(
                    deathCertificateUrl = state.deathCertificate.fileUrl,
                    familyRelationCertificateUrl = state.familyRelationCertificate.fileUrl,
                ).onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                }.onFailure { throwable ->
                    // 서류가 한 장도 없어 요청이 나가지도 않은 경우 — 서버 실패가 아니므로 리포팅하지 않는다.
                    if (throwable is DeliveryDocumentsMissingException) {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = UiText.Resource(R.string.receiver_verify_documents_required),
                            )
                        }
                        return@onFailure
                    }
                    // 서버가 사유 문구를 준 거절(이미 대기 중 등)은 예상된 경로라 리포팅하지 않는다.
                    if (throwable.shouldReportInReceiverFlow()) {
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.DELIVERY_SUBMIT, throwable)
                    }
                    _uiState.update { it.copy(isSubmitting = false) }
                    showFailure(throwable, R.string.receiver_verify_submit_failed, retry = ::submit)
                }
            }
        }

        /** 팝업의 "다시 시도하기" — 팝업을 닫고 실패한 그 요청을 그대로 다시 보낸다 (#446). */
        fun retryFailedRequest() {
            val retry = pendingRetry
            _uiState.update { it.copy(errorPopup = null) }
            pendingRetry = null
            retry?.invoke()
        }

        /** 팝업의 닫기 — 재시도 없이 화면으로 돌아간다. 붙들고 있던 시도도 함께 버린다. */
        fun onErrorPopupDismissed() {
            _uiState.update { it.copy(errorPopup = null) }
            pendingRetry = null
        }

        /**
         * 실패를 팝업(서버 작업 실패)과 스낵바(서버가 준 거절 사유) 중 한쪽으로만 보낸다 — 둘 다
         * 세우면 모달 뒤에서 스낵바가 혼자 떴다 사라진다.
         */
        private fun showFailure(
            throwable: Throwable,
            @StringRes fallbackRes: Int,
            uploadPath: Boolean = false,
            retry: () -> Unit,
        ) {
            val popup = throwable.toReceiverErrorPopupOrNull(uploadPath = uploadPath)
            pendingRetry = if (popup == null) null else retry
            _uiState.update {
                if (popup == null) {
                    it.copy(errorMessage = throwable.toReceiverErrorUiText(fallbackRes))
                } else {
                    it.copy(errorPopup = popup)
                }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(errorMessage = null) }
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
