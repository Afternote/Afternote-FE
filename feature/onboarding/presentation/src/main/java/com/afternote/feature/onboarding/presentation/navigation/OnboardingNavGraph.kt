package com.afternote.feature.onboarding.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.feature.onboarding.presentation.OnboardingProfileEntry
import com.afternote.feature.onboarding.presentation.WelcomeScreen
import com.afternote.feature.onboarding.presentation.login.LoginEntry
import com.afternote.feature.onboarding.presentation.signup.SignUpPasswordScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpResidentNumberScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel
import com.afternote.feature.onboarding.presentation.terms.OnboardingTermsScreen

/**
 * 온보딩 피처의 네비게이션 그래프.
 *
 * 플로우: Welcome -> Login / SignUp(1~4단계) -> Profile -> 완료(Home 이동)
 *
 * [NavController]는 앱 루트에서만 보유하고, 이 그래프에는 이동 이벤트만 전달합니다.
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
                onCheckRecordsClick = {
                    // TODO: 수신자 플로우 연결
                },
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

        // ── SignUp Step 1: 이메일 & 비밀번호 ──
        composable<OnboardingRoute.SignUpRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)

            SignUpScreen(
                emailState = signUpViewModel.emailState,
                passwordState = signUpViewModel.passwordState,
                isVerificationSent = signUpViewModel.isVerificationSent,
                onRequestVerification = signUpViewModel::requestVerification,
                onNextClick = actions::onSignUpEmailNext,
                onBackClick = actions::onSignUpEmailBack,
            )
        }

        // ── SignUp Step 2: 주민등록번호 ──
        composable<OnboardingRoute.SignUpResidentNumberRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)

            SignUpResidentNumberScreen(
                frontNumberState = signUpViewModel.frontNumberState,
                backNumberState = signUpViewModel.backNumberState,
                onNextClick = actions::onSignUpResidentNext,
                onBackClick = actions::onSignUpResidentBack,
            )
        }

        // ── SignUp Step 3: 비밀번호 설정 ──
        composable<OnboardingRoute.SignUpPasswordRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)

            SignUpPasswordScreen(
                passwordState = signUpViewModel.signUpPasswordState,
                passwordConfirmState = signUpViewModel.signUpPasswordConfirmState,
                onNextClick = actions::onSignUpPasswordNext,
                onBackClick = actions::onSignUpPasswordBack,
            )
        }

        // ── SignUp Step 4: 약관 동의 ──
        composable<OnboardingRoute.TermsRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)

            OnboardingTermsScreen(
                termsState = signUpViewModel.termsState,
                onTermsToggle = signUpViewModel::toggleTermsAgreed,
                onPrivacyToggle = signUpViewModel::togglePrivacyAgreed,
                onMarketingToggle = signUpViewModel::toggleMarketingAgreed,
                onToggleAll = signUpViewModel::toggleAllTerms,
                onViewTermsClick = {
                    // TODO: 약관 상세 보기 웹뷰 또는 화면 연결
                },
                onNextClick = actions::onTermsNext,
                onBackClick = actions::onTermsBack,
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
