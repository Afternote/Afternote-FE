package com.afternote.feature.onboarding.presentation.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.feature.onboarding.presentation.OnboardingProfileEntry
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.WelcomeScreen
import com.afternote.feature.onboarding.presentation.login.LoginEntry
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
                onStartClick = actions::navigateToSignUp,
                onCheckRecordsClick = actions::navigateToReceivedRecords,
                onLoginClick = actions::navigateToLogin,
            )
        }

        // ── Login ──
        composable<OnboardingRoute.LoginRoute> {
            LoginEntry(
                onLoginSuccess = actions::replaceOnboardingWithHome,
                onSignUpClick = actions::replaceLoginWithSignUp,
                onBackClick = actions::popBack,
            )
        }

        // ── SignUp Step 1: 이메일 & 인증번호 ──
        composable<OnboardingRoute.SignUpRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)
            val snackbarHostState =
                rememberSignUpEventHost(
                    viewModel = signUpViewModel,
                    onNavigateToResidentNumber = actions::proceedToSignUpResidentNumber,
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
                onBackClick = actions::popBack,
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
                onNextClick = actions::proceedToSignUpPassword,
                onBackClick = actions::popBack,
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
                onNextClick = actions::proceedToTerms,
                onBackClick = actions::popBack,
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
                onViewTermsClick = { _ -> actions.navigateToTermsDetail() },
                onNextClick = actions::proceedToProfile,
                onBackClick = actions::popBack,
            )
        }

        // ── 약관 상세 ──
        composable<OnboardingRoute.TermsDetailRoute> {
            TermsDetailScreen(
                title = "",
                onBackClick = actions::popBack,
                onNextClick = actions::popBack,
            )
        }

        // ── Profile 설정 ──
        composable<OnboardingRoute.ProfileRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)

            OnboardingProfileEntry(
                viewModel = signUpViewModel,
                onOnboardingComplete = actions::replaceOnboardingWithHome,
                onBackClick = actions::popBack,
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
 * SignUp Step 화면 공통의 Snackbar 호스트 + UI state 흡수 신호 처리.
 * 각 Step composable 에서 호출해 [SignUpViewModel.errorMessage] / [SignUpViewModel.isNameRequired]
 * 신호를 일관되게 snackbar 로 노출하고, Step 1 의 경우 [onNavigateToResidentNumber] 로 검증 통과 시 네비게이트.
 *
 * sealed Event Channel 대신 ViewModel 의 nullable/boolean 신호 + [LaunchedEffect] + on*Consumed 패턴으로 통일
 * (Google 공식 가이드 — ViewModel events should always result in a UI state update).
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

    LaunchedEffect(viewModel.shouldNavigateToResidentNumber) {
        if (viewModel.shouldNavigateToResidentNumber) {
            onNavigateToResidentNumber?.invoke()
            viewModel.onResidentNumberNavigatedConsumed()
        }
    }

    LaunchedEffect(viewModel.isNameRequired) {
        if (viewModel.isNameRequired) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = nameRequiredMessage,
                    duration = SnackbarDuration.Short,
                )
            }
            viewModel.onNameRequiredConsumed()
        }
    }

    val errorMessage = viewModel.errorMessage
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage.ifBlank { signupFailedMessage },
                    duration = SnackbarDuration.Short,
                )
            }
            viewModel.onErrorConsumed()
        }
    }

    return snackbarHostState
}
