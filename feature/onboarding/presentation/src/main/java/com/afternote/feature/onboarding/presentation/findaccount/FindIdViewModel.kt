package com.afternote.feature.onboarding.presentation.findaccount

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.reporting.AuthFailureStage
import com.afternote.feature.onboarding.presentation.reporting.recordAuthFailure
import com.afternote.feature.onboarding.presentation.toDisplayMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * 아이디 찾기 플로우 ViewModel. 인증 화면과 결과 화면이 `Route.Onboarding` 그래프 스코프로 공유한다
 * (결과 화면이 [FindIdUiState.foundAccount] 를 읽어야 해서 Route 인자 대신 VM 공유를 택했다 —
 * 회원가입 플로우가 이미 같은 방식이다).
 *
 * `TextFieldState` 는 Screen 이 소유하고 VM 은 String 만 들고 있는다.
 *
 * 전이는 [reduce] 한 곳이고 진입점은 [onIntent] 하나다 (#1802). 기준은
 * `docs/convention/mvi.md`.
 */
@HiltViewModel
class FindIdViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<FindIdIntent, FindIdUiState, FindIdReducerEvent>(FindIdUiState()) {
        private var cooldownJob: Job? = null

        override fun onIntent(intent: FindIdIntent) {
            when (intent) {
                is FindIdIntent.UpdateEmail -> dispatch(FindIdReducerEvent.EmailChanged(intent.value))
                is FindIdIntent.UpdateCertificateCode -> dispatch(FindIdReducerEvent.CertificateCodeChanged(intent.value))
                FindIdIntent.RequestVerificationCode -> requestVerificationCode()
                FindIdIntent.VerifyCode -> verifyCode()
                FindIdIntent.ConsumeError -> dispatch(FindIdReducerEvent.ErrorConsumed)
            }
        }

        override fun reduce(
            state: FindIdUiState,
            event: FindIdReducerEvent,
        ): FindIdUiState =
            when (event) {
                // 이메일이 바뀌면 앞서 받은 계정·에러는 더 이상 그 이메일의 것이 아니다.
                is FindIdReducerEvent.EmailChanged -> {
                    state.copy(email = event.value, foundAccount = null, hasVerificationError = false)
                }

                is FindIdReducerEvent.CertificateCodeChanged -> {
                    state.copy(certificateCode = event.value, hasVerificationError = false)
                }

                FindIdReducerEvent.CodeSendStarted -> {
                    state.copy(isSendingCode = true)
                }

                FindIdReducerEvent.CodeSent -> {
                    state.copy(isVerificationSent = true, hasVerificationError = false)
                }

                is FindIdReducerEvent.CodeSendFailed -> {
                    state.copy(errorMessage = event.message)
                }

                FindIdReducerEvent.CodeSendFinished -> {
                    state.copy(isSendingCode = false)
                }

                FindIdReducerEvent.CooldownReloaded -> {
                    state.copy(resendCooldownSeconds = RESEND_COOLDOWN_SECONDS)
                }

                FindIdReducerEvent.CooldownTicked -> {
                    state.copy(resendCooldownSeconds = state.resendCooldownSeconds - 1)
                }

                FindIdReducerEvent.VerifyStarted -> {
                    state.copy(isVerifying = true, hasVerificationError = false)
                }

                is FindIdReducerEvent.AccountFound -> {
                    state.copy(foundAccount = event.account)
                }

                // 스낵바 신호를 함께 내린다 — 이번 실패는 인라인으로 알리므로, 아직 소비되지 않은
                // 이전 실패 문구가 인라인과 겹쳐 뜨지 않게 한다.
                FindIdReducerEvent.VerificationRejected -> {
                    state.copy(hasVerificationError = true, errorMessage = null)
                }

                is FindIdReducerEvent.VerifyFailed -> {
                    state.copy(errorMessage = event.message)
                }

                FindIdReducerEvent.VerifyFinished -> {
                    state.copy(isVerifying = false)
                }

                FindIdReducerEvent.ErrorConsumed -> {
                    state.copy(errorMessage = null)
                }
            }

        private fun requestVerificationCode() {
            val state = currentState
            if (!state.isSendCodeEnabled) return
            viewModelScope.launch {
                dispatch(FindIdReducerEvent.CodeSendStarted)
                accountRepository
                    .sendFindCode(state.email)
                    .onSuccess {
                        dispatch(FindIdReducerEvent.CodeSent)
                        startResendCooldown()
                    }.onFailure { error ->
                        // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                        if (error is CancellationException) throw error
                        errorReporter.recordAuthFailure(AuthFailureStage.FIND_ACCOUNT_CODE_SEND, error)
                        dispatch(
                            FindIdReducerEvent.CodeSendFailed(
                                error.toDisplayMessage(R.string.onboarding_find_account_failed),
                            ),
                        )
                    }
                dispatch(FindIdReducerEvent.CodeSendFinished)
            }
        }

        /**
         * 인증번호 "확인". 찾기 흐름에는 검증 전용 엔드포인트가 없고 `auth/email/find` 가
         * 인증번호까지 함께 받아 계정을 돌려주므로, 확인 = 검증 + 조회를 한 번에 한다.
         * 결과는 [FindIdUiState.foundAccount] 에 담아두고 "다음" 에서 결과 화면이 소비한다.
         *
         * 실패는 두 갈래로 나뉜다 — 인증번호 무효([CoreAuthFailure.EmailVerification], 서버 code 1207)만
         * 필드 아래 인라인 문구이고, 그 밖의 실패는 사용자가 입력으로 고칠 수 없어 스낵바로 보낸다.
         * 갈래를 나누지 않으면 네트워크 실패에도 "인증번호가 일치하지 않습니다" 가 뜬다.
         */
        private fun verifyCode() {
            val state = currentState
            if (!state.isVerifyEnabled) return
            viewModelScope.launch {
                dispatch(FindIdReducerEvent.VerifyStarted)
                accountRepository
                    .findAccount(state.email, state.certificateCode)
                    .onSuccess { account ->
                        dispatch(FindIdReducerEvent.AccountFound(account))
                    }.onFailure { error ->
                        // 취소는 장애가 아니다 — 여기는 계측 대상이 아니지만 실패 UI 로 소비하는 것도
                        // 막아야 해서 되던진다(전수 정정은 #661).
                        if (error is CancellationException) throw error
                        // 계측하지 않는다 — 인증번호 오타는 사용자의 정상적인 입력 실수다.
                        // 자세한 사유는 AuthFailureStage.FIND_ACCOUNT_CODE_SEND KDoc.
                        if (error is CoreAuthFailure.EmailVerification) {
                            dispatch(FindIdReducerEvent.VerificationRejected)
                        } else {
                            dispatch(
                                FindIdReducerEvent.VerifyFailed(
                                    error.toDisplayMessage(R.string.onboarding_find_account_failed),
                                ),
                            )
                        }
                    }
                dispatch(FindIdReducerEvent.VerifyFinished)
            }
        }

        private fun startResendCooldown() {
            // 중복 방지가 아니라 last-wins 재장전 — 이전 카운트다운을 폐기하고 30초를 새로 센다.
            // 호출부 가드(isSendCodeEnabled)상 보통 이전 잡은 끝나 있지만, "쿨다운 잡 최대 1개" 를
            // 함수 스스로 보장해 가드가 느슨해져도 2배속 감산이 생기지 않게 한다.
            cooldownJob?.cancel()
            cooldownJob =
                viewModelScope.launch {
                    dispatch(FindIdReducerEvent.CooldownReloaded)
                    while (currentState.resendCooldownSeconds > 0) {
                        delay(MILLIS_PER_SECOND.milliseconds)
                        dispatch(FindIdReducerEvent.CooldownTicked)
                    }
                }
        }

        private companion object {
            // 시안·서버 계약에 없는 클라 관례값 — 회원가입(SignUpViewModel)의 쿨다운과 동일하게 맞춤.
            const val RESEND_COOLDOWN_SECONDS = 30
            const val MILLIS_PER_SECOND = 1000L
        }
    }
