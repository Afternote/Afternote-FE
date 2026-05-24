package com.afternote.feature.onboarding.presentation.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.ObserveAsEvents
import com.afternote.core.ui.Route
import com.afternote.feature.onboarding.presentation.OnboardingProfileEntry
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.WelcomeScreen
import com.afternote.feature.onboarding.presentation.login.LoginEntry
import com.afternote.feature.onboarding.presentation.signup.SignUpEvent
import com.afternote.feature.onboarding.presentation.signup.SignUpPasswordScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpResidentNumberScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel
import com.afternote.feature.onboarding.presentation.terms.OnboardingTermsScreen
import com.afternote.feature.onboarding.presentation.terms.TermsDetailScreen
import kotlinx.coroutines.launch

/**
 * 온보딩 피처의 네비게이션 그래프.
 *
 * 플로우: Welcome -> Login / SignUp(1~4단계) -> Profile -> 완료(Home 이동)
 */
fun NavGraphBuilder.onboardingNavGraph(
    /** [Route.Onboarding] 그래프 엔트리 — SignUp/Profile 스코프 ViewModel 바인딩에 사용 */
    graphScopedParentEntry: () -> NavBackStackEntry,
    actions: OnboardingNavActions,
) {
    navigation<Route.Onboarding>(startDestination = OnboardingRoute.WelcomeRoute) {
        // ── Welcome ──
        composable<OnboardingRoute.WelcomeRoute> {
            WelcomeScreen(
                onStartClick = actions::onNavigateWelcomeToSignUp,
                onCheckRecordsClick = actions::onNavigateWelcomeToReceivedRecords,
                onLoginClick = actions::onNavigateWelcomeToLogin,
            )
        }

        // ── Login ──
        composable<OnboardingRoute.LoginRoute> {
            LoginEntry(
                onLoginSuccess = actions::onOnboardingComplete,
                onSignUpClick = actions::onReplaceLoginWithSignUp,
                onBackClick = actions::onLoginBack,
            )
        }

        // ── SignUp Step 1: 이메일 & 인증번호 ──
        composable<OnboardingRoute.SignUpRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)
            val snackbarHostState =
                rememberSignUpEventHost(
                    viewModel = signUpViewModel,
                    onNavigateToResidentNumber = actions::onSignUpEmailNext,
                )

            SignUpScreen(
                initialEmail = signUpViewModel.email,
                initialVerificationCode = signUpViewModel.verificationCode,
                isVerificationSent = signUpViewModel.isVerificationSent,
                isSendingCode = signUpViewModel.isSendingCode,
                isEmailFormatValid = signUpViewModel.isEmailFormatValid,
                resendCooldownSeconds = signUpViewModel.resendCooldownSeconds,
                verificationRemainingSeconds = signUpViewModel.verificationRemainingSeconds,
                isNextEnabled = signUpViewModel.isStep1NextEnabled,
                snackbarHostState = snackbarHostState,
                onEmailChange = signUpViewModel::updateEmail,
                onVerificationCodeChange = signUpViewModel::updateVerificationCode,
                onRequestVerification = signUpViewModel::requestVerification,
                onNextClick = signUpViewModel::verifyEmailAndProceed,
                onBackClick = actions::onSignUpEmailBack,
            )
        }

        // ── SignUp Step 2: 주민등록번호 ──
        composable<OnboardingRoute.SignUpResidentNumberRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)
            val snackbarHostState = rememberSignUpEventHost(signUpViewModel)

            SignUpResidentNumberScreen(
                initialFrontNumber = signUpViewModel.residentFrontNumber,
                initialBackNumber = signUpViewModel.residentBackNumber,
                isNextEnabled = signUpViewModel.isStep2NextEnabled,
                snackbarHostState = snackbarHostState,
                onFrontNumberChange = signUpViewModel::updateResidentFrontNumber,
                onBackNumberChange = signUpViewModel::updateResidentBackNumber,
                onNextClick = actions::onSignUpResidentNext,
                onBackClick = actions::onSignUpResidentBack,
            )
        }

        // ── SignUp Step 3: 비밀번호 설정 ──
        composable<OnboardingRoute.SignUpPasswordRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)
            val snackbarHostState = rememberSignUpEventHost(signUpViewModel)

            SignUpPasswordScreen(
                initialPassword = signUpViewModel.signUpPassword,
                initialPasswordConfirm = signUpViewModel.signUpPasswordConfirm,
                isPasswordRuleSatisfied = signUpViewModel.isPasswordRuleSatisfied,
                isNextEnabled = signUpViewModel.isStep3NextEnabled,
                snackbarHostState = snackbarHostState,
                onPasswordChange = signUpViewModel::updateSignUpPassword,
                onPasswordConfirmChange = signUpViewModel::updateSignUpPasswordConfirm,
                onNextClick = actions::onSignUpPasswordNext,
                onBackClick = actions::onSignUpPasswordBack,
            )
        }

        // ── SignUp Step 4: 약관 동의 ──
        composable<OnboardingRoute.TermsRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)
            val snackbarHostState = rememberSignUpEventHost(signUpViewModel)

            OnboardingTermsScreen(
                termsState = signUpViewModel.termsState,
                isNextEnabled = signUpViewModel.isStep4NextEnabled,
                snackbarHostState = snackbarHostState,
                onTermsToggle = signUpViewModel::toggleTermsAgreed,
                onPrivacyToggle = signUpViewModel::togglePrivacyAgreed,
                onMarketingToggle = signUpViewModel::toggleMarketingAgreed,
                onToggleAll = signUpViewModel::toggleAllTerms,
                onViewTermsClick = { _ -> actions.onViewTerms() },
                onNextClick = actions::onTermsNext,
                onBackClick = actions::onTermsBack,
            )
        }

        // ── 약관 상세 ──
        composable<OnboardingRoute.TermsDetailRoute> {
            TermsDetailScreen(
                title = "",
                onBackClick = actions::onTermsDetailBack,
                onNextClick = actions::onTermsDetailBack,
            )
        }

        // ── Profile 설정 ──
        composable<OnboardingRoute.ProfileRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)

            OnboardingProfileEntry(
                viewModel = signUpViewModel,
                onOnboardingComplete = actions::onOnboardingComplete,
                onBackClick = actions::onProfileBack,
            )
        }
    }
}

/**
 * `Route.Onboarding` 그래프 스코프에 묶인 [SignUpViewModel]을 가져옵니다.
 * SignUp Step 1~4와 Profile 화면이 동일한 인스턴스를 공유합니다.
 */
@Composable
private fun graphScopedSignUpViewModel(graphScopedParentEntry: () -> NavBackStackEntry): SignUpViewModel {
    val parentEntry = remember { graphScopedParentEntry() }
    return hiltViewModel(parentEntry)
}

/**
 * SignUp Step 화면 공통의 Snackbar 호스트 + 이벤트 수집기.
 * 각 Step composable 에서 호출해 [SignUpEvent.ShowError] 를 일관되게 노출하고,
 * Step 1 의 경우 [onNavigateToResidentNumber] 로 검증 통과 시 네비게이트한다.
 */
@Composable
private fun rememberSignUpEventHost(
    viewModel: SignUpViewModel,
    onNavigateToResidentNumber: (() -> Unit)? = null,
): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val signupFailedMessage = stringResource(R.string.signup_failed)
    val nameRequiredMessage = stringResource(R.string.signup_name_required)

    ObserveAsEvents(viewModel.eventFlow) { event ->
        when (event) {
            SignUpEvent.NavigateToResidentNumber -> {
                onNavigateToResidentNumber?.invoke()
            }

            SignUpEvent.NameRequired -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = nameRequiredMessage,
                        duration = SnackbarDuration.Short,
                    )
                }
            }

            is SignUpEvent.ShowError -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = event.message ?: signupFailedMessage,
                        duration = SnackbarDuration.Short,
                    )
                }
            }

            SignUpEvent.SignUpSuccess -> {
                Unit
            } // Profile 화면(OnboardingProfileEntry)에서 처리
        }
    }

    return snackbarHostState
}
