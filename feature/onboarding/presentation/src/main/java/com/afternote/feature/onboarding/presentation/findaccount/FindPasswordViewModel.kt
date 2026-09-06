package com.afternote.feature.onboarding.presentation.findaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.reporting.AuthFailureStage
import com.afternote.feature.onboarding.presentation.reporting.recordAuthFailure
import com.afternote.feature.onboarding.presentation.toDisplayMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * 비밀번호 찾기 플로우 ViewModel — 이메일 인증 · 비밀번호 변경 · 완료 세 화면이
 * `Route.Onboarding` 그래프 스코프로 공유한다.
 *
 * 공유가 필요한 이유는 [FindPasswordUiState.certificateCode] 다. 서버는 인증번호와 새 비밀번호를
 * **한 요청**(`auth/password/find`)으로 받으므로 인증 화면에서 받은 코드를 비밀번호 변경 화면까지
 * 들고 가야 한다. Route 인자로 넘기지 않는 것은 회원가입·아이디 찾기와 같은 관례이자,
 * 인증번호가 백스택 엔트리 인자로 남지 않게 하려는 것이기도 하다.
 *
 * `TextFieldState` 는 Screen 이 소유하고 VM 은 String 만 들고 있는다.
 */
@HiltViewModel
class FindPasswordViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FindPasswordUiState())
        val uiState: StateFlow<FindPasswordUiState> = _uiState.asStateFlow()

        private var cooldownJob: Job? = null

        /**
         * 진행 중인 서버 호출. 상태 플래그만으로는 연타를 막지 못한다 — `viewModelScope.launch` 가
         * 비동기라 `isSendingCode`·`isSubmitting` 이 반영되기 전에 두 번째 호출이 같은 `false` 를
         * 읽고 통과한다(`SignUpViewModel.signUpJob` 과 같은 판단).
         *
         * 두 흐름 다 인증번호를 소비하므로 중복 요청이 그냥 낭비로 끝나지 않는다 — 발송은 코드를
         * 새로 발급해 앞서 안내한 것을 무효로 만들고, 재설정은 두 번째가 1207 로 실패해 첫 요청이
         * 성공했는데도 "이메일 인증부터 다시" 안내가 뜬다.
         */
        private var sendCodeJob: Job? = null
        private var resetJob: Job? = null

        fun updateEmail(value: String) =
            _uiState.update {
                // 이메일이 바뀌면 앞선 발송 이력과 차단 판정은 더 이상 그 이메일의 것이 아니다.
                it.copy(email = value, isVerificationSent = false, isSocialSignUpAccount = false)
            }

        fun updateCertificateCode(value: String) = _uiState.update { it.copy(certificateCode = value) }

        fun updateNewPassword(value: String) = _uiState.update { it.copy(newPassword = value) }

        fun updateNewPasswordConfirm(value: String) = _uiState.update { it.copy(newPasswordConfirm = value) }

        /**
         * 인증번호 발송. 소셜 가입 계정은 여기서 걸린다 — 서버가 `auth/find/send/code` 에서
         * 로컬 계정만 통과시키므로(code 1702), 코드 입력 전에 차단 팝업을 낼 수 있다.
         */
        fun requestVerificationCode() {
            if (sendCodeJob?.isActive == true) return
            val state = _uiState.value
            if (!state.isSendCodeEnabled) return
            sendCodeJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isSendingCode = true) }
                    accountRepository
                        .sendFindCode(state.email)
                        .onSuccess {
                            _uiState.update { it.copy(isVerificationSent = true) }
                            startResendCooldown()
                        }.onFailure { error ->
                            // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                            if (error is CancellationException) throw error
                            _uiState.update { current ->
                                if (error is CoreAuthFailure.SocialSignUpAccount) {
                                    // 사용자 입력 오류가 아니라 계정 종류의 문제다 — 시안은 팝업으로 그린다.
                                    // 계측하지 않는다: 서버가 정상적으로 가르는 분기지 장애가 아니다.
                                    //
                                    // 스낵바 신호를 함께 내린다 — 이 사유는 팝업으로 알리므로, 아직 소비되지
                                    // 않은 이전 실패 문구가 팝업과 겹쳐 뜨지 않게 한다(`FindIdViewModel.verifyCode`
                                    // 선례). `errorMessage` 를 지우는 경로가 [onErrorConsumed] 하나뿐이라
                                    // 이메일을 고쳐 재발송해도 남는다 — `current.copy` 가 그대로 물려받는다.
                                    current.copy(isSocialSignUpAccount = true, errorMessage = null)
                                } else {
                                    errorReporter.recordAuthFailure(AuthFailureStage.FIND_ACCOUNT_CODE_SEND, error)
                                    current.copy(errorMessage = error.toDisplayMessage(R.string.onboarding_find_account_failed))
                                }
                            }
                        }
                    _uiState.update { it.copy(isSendingCode = false) }
                }
        }

        /**
         * 최종 제출 — 인증번호 검증과 비밀번호 반영이 이 한 번의 호출에서 함께 일어난다.
         *
         * 인증번호 무효([CoreAuthFailure.EmailVerification])만 전용 문구로 가른다. 이 시점의
         * 1207 은 "오타" 가 아니라 대개 만료라서, 폴백 문구로는 사용자가 무엇을 다시 해야 하는지
         * 알 수 없다 — 이메일 인증부터 다시 하라고 말해 준다. 시안에는 이 상태가 없어(변경 화면에
         * 인라인 에러 슬롯이 없다) 스낵바로 낸다.
         */
        fun submitNewPassword() {
            if (resetJob?.isActive == true) return
            val state = _uiState.value
            if (!state.isResetEnabled) return
            resetJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isSubmitting = true) }
                    accountRepository
                        .resetPassword(
                            email = state.email,
                            certificateCode = state.certificateCode,
                            newPassword = state.newPassword,
                            confirmPassword = state.newPasswordConfirm,
                        ).onSuccess {
                            _uiState.update { it.copy(isPasswordChanged = true) }
                        }.onFailure { error ->
                            if (error is CancellationException) throw error
                            errorReporter.recordAuthFailure(AuthFailureStage.FIND_PASSWORD_RESET, error)
                            _uiState.update {
                                it.copy(errorMessage = error.toResetFailureMessage())
                            }
                        }
                    _uiState.update { it.copy(isSubmitting = false) }
                }
        }

        fun onSocialAccountBlockedConsumed() = _uiState.update { it.copy(isSocialSignUpAccount = false) }

        fun onErrorConsumed() = _uiState.update { it.copy(errorMessage = null) }

        /**
         * 완료 화면으로 넘어간 뒤 흐름 상태를 통째로 버린다.
         *
         * 그래프 스코프 VM 이라 인스턴스가 흐름보다 오래 산다 — 초기화하지 않으면 (1) 재진입 시
         * [FindPasswordUiState.isPasswordChanged] 가 참인 채로 남아 완료 화면으로 곧장 튀고,
         * (2) 평문 비밀번호가 메모리에 계속 남는다.
         */
        fun onPasswordResetConsumed() {
            cooldownJob?.cancel()
            cooldownJob = null
            _uiState.value = FindPasswordUiState()
        }

        private fun startResendCooldown() {
            // 중복 방지가 아니라 last-wins 재장전 — 이전 카운트다운을 폐기하고 30초를 새로 센다.
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

        private fun Throwable.toResetFailureMessage(): UiText =
            if (this is CoreAuthFailure.EmailVerification) {
                UiText.Resource(R.string.onboarding_find_password_code_expired)
            } else {
                toDisplayMessage(R.string.onboarding_find_password_failed)
            }

        private companion object {
            // 시안·서버 계약에 없는 클라 관례값 — 회원가입·아이디 찾기의 쿨다운과 동일하게 맞춤.
            const val RESEND_COOLDOWN_SECONDS = 30
            const val MILLIS_PER_SECOND = 1000L
        }
    }
