package com.afternote.feature.onboarding.presentation.findaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.account.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * 아이디 찾기 플로우 ViewModel. 인증 화면과 결과 화면이 `Route.Onboarding` 그래프 스코프로 공유한다
 * (결과 화면이 [FindIdUiState.foundAccount] 를 읽어야 해서 Route 인자 대신 VM 공유를 택했다 —
 * 회원가입 플로우가 이미 같은 방식이다).
 *
 * `TextFieldState` 는 Screen 이 소유하고 VM 은 String 만 들고 있는다.
 */
@HiltViewModel
class FindIdViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FindIdUiState())
        val uiState: StateFlow<FindIdUiState> = _uiState.asStateFlow()

        private var cooldownJob: Job? = null

        fun updateEmail(value: String) =
            _uiState.update {
                // 이메일이 바뀌면 앞서 받은 계정·에러는 더 이상 그 이메일의 것이 아니다.
                it.copy(email = value, foundAccount = null, verificationError = null)
            }

        fun updateCertificateCode(value: String) = _uiState.update { it.copy(certificateCode = value, verificationError = null) }

        fun requestVerificationCode() {
            val state = _uiState.value
            if (!state.isSendCodeEnabled) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSendingCode = true) }
                accountRepository
                    .sendFindCode(state.email)
                    .onSuccess {
                        _uiState.update { it.copy(isVerificationSent = true, verificationError = null) }
                        startResendCooldown()
                    }.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message ?: "") }
                    }
                _uiState.update { it.copy(isSendingCode = false) }
            }
        }

        /**
         * 인증번호 "확인". 찾기 흐름에는 검증 전용 엔드포인트가 없고 `auth/email/find` 가
         * 인증번호까지 함께 받아 계정을 돌려주므로, 확인 = 검증 + 조회를 한 번에 한다.
         * 결과는 [FindIdUiState.foundAccount] 에 담아두고 "다음" 에서 결과 화면이 소비한다.
         */
        fun verifyCode() {
            val state = _uiState.value
            if (!state.isVerifyEnabled) return
            viewModelScope.launch {
                _uiState.update { it.copy(isVerifying = true, verificationError = null) }
                accountRepository
                    .findAccount(state.email, state.certificateCode)
                    .onSuccess { account ->
                        _uiState.update { it.copy(foundAccount = account) }
                    }.onFailure { error ->
                        _uiState.update { it.copy(verificationError = error.message ?: "") }
                    }
                _uiState.update { it.copy(isVerifying = false) }
            }
        }

        fun onErrorConsumed() = _uiState.update { it.copy(errorMessage = null) }

        private fun startResendCooldown() {
            // 중복 방지가 아니라 last-wins 재장전 — 이전 카운트다운을 폐기하고 30초를 새로 센다.
            // 호출부 가드(isSendCodeEnabled)상 보통 이전 잡은 끝나 있지만, "쿨다운 잡 최대 1개" 를
            // 함수 스스로 보장해 가드가 느슨해져도 2배속 감산이 생기지 않게 한다.
            cooldownJob?.cancel()
            cooldownJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(resendCooldownSeconds = RESEND_COOLDOWN_SECONDS) }
                    while (_uiState.value.resendCooldownSeconds > 0) {
                        delay(MILLIS_PER_SECOND.milliseconds)
                        _uiState.update { it.copy(resendCooldownSeconds = it.resendCooldownSeconds - 1) }
                    }
                }
        }

        private companion object {
            // 시안·서버 계약에 없는 클라 관례값 — 회원가입(SignUpViewModel)의 쿨다운과 동일하게 맞춤.
            const val RESEND_COOLDOWN_SECONDS = 30
            const val MILLIS_PER_SECOND = 1000L
        }
    }
