package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 본인 확인 이메일 인증(designs 3·4) ViewModel — 인증번호 발송 + 코드 검증 (이슈 #215).
 *
 * 백엔드 미구현 단계라 [IdentityEmailVerificationStub] 으로 시뮬레이션한다. 검증 성공 시
 * [IdentityVerificationRepository.markVerified] 로 캐시를 켜고 [IdentityVerificationEvent.Verified] 발행 →
 * UI 가 마스터 키(5) 단계로 이동.
 *
 * 메모리 정책상 ViewModel 은 [androidx.compose.foundation.text.input.TextFieldState] 를 보유하지 않는다.
 * UI 가 입력값을 [onEmailChange]·[onCodeChange] 로 흘려주고 본 VM 은 String 만 관리.
 */
@HiltViewModel
class IdentityVerificationViewModel
    @Inject
    constructor(
        private val stub: IdentityEmailVerificationStub,
        private val identityVerificationRepository: IdentityVerificationRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(IdentityVerificationUiState())
        val uiState: StateFlow<IdentityVerificationUiState> = _uiState.asStateFlow()

        fun onEmailChange(value: String) {
            _uiState.update {
                it.copy(
                    email = value,
                    isEmailFormatValid = EMAIL_REGEX.matches(value.trim()),
                    errorMessageRes = null,
                )
            }
        }

        fun onCodeChange(value: String) {
            _uiState.update { it.copy(code = value, errorMessageRes = null) }
        }

        fun requestVerificationCode() {
            val state = _uiState.value
            if (!state.isEmailFormatValid || state.isSendingCode) return
            _uiState.update { it.copy(isSendingCode = true, errorMessageRes = null) }
            viewModelScope.launch {
                stub
                    .sendCode(state.email)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isSendingCode = false,
                                isVerificationSent = true,
                            )
                        }
                    }.onFailure {
                        _uiState.update {
                            it.copy(
                                isSendingCode = false,
                                errorMessageRes = R.string.receiver_verify_email_format_invalid,
                            )
                        }
                    }
            }
        }

        fun verifyAndProceed() {
            val state = _uiState.value
            if (!state.canSubmit) return
            _uiState.update { it.copy(isVerifying = true, errorMessageRes = null) }
            viewModelScope.launch {
                stub
                    .verifyCode(state.email, state.code)
                    .onSuccess {
                        identityVerificationRepository.markVerified()
                        _uiState.update { it.copy(isVerifying = false, isVerified = true) }
                    }.onFailure {
                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                errorMessageRes = R.string.receiver_verify_code_mismatch,
                            )
                        }
                    }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(errorMessageRes = null) }
        }

        fun onVerifiedConsumed() {
            _uiState.update { it.copy(isVerified = false) }
        }

        private companion object {
            val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        }
    }
