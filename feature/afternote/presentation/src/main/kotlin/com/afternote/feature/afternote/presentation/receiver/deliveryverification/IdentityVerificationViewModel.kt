package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.error.ReceiverServerRejectionException
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import com.afternote.feature.afternote.presentation.reporting.shouldReportInReceiverFlow
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 본인 확인 이메일 인증(designs 3·4) ViewModel — 인증번호 발송 + 코드 검증 (이슈 #215, #407).
 *
 * [ReceiverAuthRepository.sendEmailAuthCode]·[ReceiverAuthRepository.verifyEmailAuthCode] 로
 * 실 API(`receiver-auth/email` 계열) 를 호출한다. 서버가 안내한 4xx 거절(이메일 미등록·인증번호
 * 만료/불일치 등) 의 문구는 [ReceiverServerRejectionException.serverMessage] 를 그대로 노출하고,
 * 그 외(5xx 장애·인프라 실패)는 정적 리소스로 폴백 — 판정은 [toErrorPayload] 가 가른다 (#651).
 *
 * 검증 성공 시 [IdentityVerificationRepository.markVerified] 로 캐시를 켜고 isVerified 신호 발행 →
 * UI 가 마스터 키(5) 단계로 이동. 이메일 인증은 신원 확인까지만 담당하며 마스터 키를 대신 획득하지
 * 않는다 — 그랬다면 마스터 키 단계가 무력화된다 (#454).
 *
 * 메모리 정책상 ViewModel 은 [androidx.compose.foundation.text.input.TextFieldState] 를 보유하지 않는다.
 * UI 가 입력값을 [onEmailChange]·[onCodeChange] 로 흘려주고 본 VM 은 String 만 관리.
 *
 * 입력 trim 은 presentation(본 VM) 책임 — 사용자 실수 공백 제거는 입력 UX 보정이지 비즈니스 규칙이
 * 아니라 domain/data 로 내리지 않는다. state 에는 raw 를 두고(타이핑 중간 상태 보존) 검증·전송 시점에 적용.
 */
@HiltViewModel
class IdentityVerificationViewModel
    @Inject
    constructor(
        private val receiverAuthRepository: ReceiverAuthRepository,
        private val identityVerificationRepository: IdentityVerificationRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(IdentityVerificationUiState())
        val uiState: StateFlow<IdentityVerificationUiState> = _uiState.asStateFlow()

        fun onEmailChange(value: String) {
            _uiState.update {
                it.copy(
                    email = value,
                    isEmailFormatValid = EMAIL_REGEX.matches(value.trim()),
                    error = null,
                )
            }
        }

        fun onCodeChange(value: String) {
            _uiState.update { it.copy(code = value, error = null) }
        }

        fun requestVerificationCode() {
            val state = _uiState.value
            if (!state.isEmailFormatValid || state.isSendingCode) return
            _uiState.update { it.copy(isSendingCode = true, error = null) }
            viewModelScope.launch {
                receiverAuthRepository
                    .sendEmailAuthCode(state.email.trim())
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isSendingCode = false,
                                isVerificationSent = true,
                            )
                        }
                    }.onFailure { throwable ->
                        if (throwable.shouldReportInReceiverFlow()) {
                            errorReporter.recordAfternoteFailure(
                                AfternoteFailureStage.RECEIVER_EMAIL_CODE_SEND,
                                throwable,
                            )
                        }
                        _uiState.update {
                            it.copy(
                                isSendingCode = false,
                                error = throwable.toErrorPayload(R.string.receiver_verify_code_send_failed),
                            )
                        }
                    }
            }
        }

        fun verifyAndProceed() {
            val state = _uiState.value
            if (!state.canSubmit) return
            _uiState.update { it.copy(isVerifying = true, error = null) }
            viewModelScope.launch {
                receiverAuthRepository
                    .verifyEmailAuthCode(email = state.email.trim(), authCode = state.code.trim())
                    .onSuccess {
                        identityVerificationRepository.markVerified()
                        _uiState.update { it.copy(isVerifying = false, isVerified = true) }
                    }.onFailure { throwable ->
                        if (throwable.shouldReportInReceiverFlow()) {
                            errorReporter.recordAfternoteFailure(
                                AfternoteFailureStage.RECEIVER_EMAIL_VERIFY,
                                throwable,
                            )
                        }
                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                error = throwable.toErrorPayload(R.string.receiver_verify_code_verify_failed),
                            )
                        }
                    }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(error = null) }
        }

        fun onVerifiedConsumed() {
            _uiState.update { it.copy(isVerified = false) }
        }

        private companion object {
            val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        }
    }
