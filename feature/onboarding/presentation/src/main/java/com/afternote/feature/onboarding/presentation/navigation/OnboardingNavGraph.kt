package com.afternote.feature.onboarding.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
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
 */
fun NavGraphBuilder.onboardingNavGraph(
    navController: NavController,
    onOnboardingComplete: () -> Unit,
) {
    navigation<Route.Onboarding>(startDestination = OnboardingRoute.WelcomeRoute) {
        // ── Welcome ──
        composable<OnboardingRoute.WelcomeRoute> {
            WelcomeScreen(
                onStartClick = {
                    navController.navigate(OnboardingRoute.SignUpRoute)
                },
                onCheckRecordsClick = {
                    // TODO: 수신자 플로우 연결
                },
                onLoginClick = {
                    navController.navigate(OnboardingRoute.LoginRoute)
                },
            )
        }

        // ── Login ──
        composable<OnboardingRoute.LoginRoute> {
            LoginEntry(
                onLoginSuccess = onOnboardingComplete,
                onSignUpClick = {
                    navController.navigate(OnboardingRoute.SignUpRoute) {
                        popUpTo<OnboardingRoute.LoginRoute> { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── SignUp Step 1: 이메일 & 비밀번호 ──
        composable<OnboardingRoute.SignUpRoute> { backStackEntry ->
            val signUpViewModel = graphScopedSignUpViewModel(navController, backStackEntry)

            SignUpScreen(
                emailState = signUpViewModel.emailState,
                passwordState = signUpViewModel.passwordState,
                isVerificationSent = signUpViewModel.isVerificationSent,
                onRequestVerification = signUpViewModel::requestVerification,
                onNextClick = {
                    navController.navigate(OnboardingRoute.SignUpResidentNumberRoute)
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── SignUp Step 2: 주민등록번호 ──
        composable<OnboardingRoute.SignUpResidentNumberRoute> { backStackEntry ->
            val signUpViewModel = graphScopedSignUpViewModel(navController, backStackEntry)

            SignUpResidentNumberScreen(
                frontNumberState = signUpViewModel.frontNumberState,
                backNumberState = signUpViewModel.backNumberState,
                onNextClick = {
                    navController.navigate(OnboardingRoute.SignUpPasswordRoute)
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── SignUp Step 3: 비밀번호 설정 ──
        composable<OnboardingRoute.SignUpPasswordRoute> { backStackEntry ->
            val signUpViewModel = graphScopedSignUpViewModel(navController, backStackEntry)

            SignUpPasswordScreen(
                passwordState = signUpViewModel.signUpPasswordState,
                passwordConfirmState = signUpViewModel.signUpPasswordConfirmState,
                onNextClick = {
                    navController.navigate(OnboardingRoute.TermsRoute)
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── SignUp Step 4: 약관 동의 ──
        composable<OnboardingRoute.TermsRoute> { backStackEntry ->
            val signUpViewModel = graphScopedSignUpViewModel(navController, backStackEntry)

            OnboardingTermsScreen(
                termsState = signUpViewModel.termsState,
                onTermsToggle = signUpViewModel::toggleTermsAgreed,
                onPrivacyToggle = signUpViewModel::togglePrivacyAgreed,
                onMarketingToggle = signUpViewModel::toggleMarketingAgreed,
                onToggleAll = signUpViewModel::toggleAllTerms,
                onViewTermsClick = {
                    // TODO: 약관 상세 보기 웹뷰 또는 화면 연결
                },
                onNextClick = {
                    navController.navigate(OnboardingRoute.ProfileRoute)
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        // ── Profile 설정 ──
        composable<OnboardingRoute.ProfileRoute> { backStackEntry ->
            val signUpViewModel = graphScopedSignUpViewModel(navController, backStackEntry)

            OnboardingProfileEntry(
                viewModel = signUpViewModel,
                onOnboardingComplete = onOnboardingComplete,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

/**
 * `Route.Onboarding` 그래프 스코프에 묶인 [SignUpViewModel]을 가져옵니다.
 * SignUp Step 1~4와 Profile 화면이 동일한 인스턴스를 공유합니다.
 */
@Composable
private fun graphScopedSignUpViewModel(
    navController: NavController,
    currentEntry: NavBackStackEntry,
): SignUpViewModel {
    val parentEntry =
        remember(currentEntry) {
            navController.getBackStackEntry<Route.Onboarding>()
        }
    return hiltViewModel(parentEntry)
}
