package com.afternote.feature.onboarding.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.feature.onboarding.presentation.OnboardingProfileScreen
import com.afternote.feature.onboarding.presentation.WelcomeScreen
import com.afternote.feature.onboarding.presentation.findaccount.FindIdScreen
import com.afternote.feature.onboarding.presentation.findaccount.FindIdViewModel
import com.afternote.feature.onboarding.presentation.login.LoginScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpPasswordScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpResidentNumberScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel
import com.afternote.feature.onboarding.presentation.terms.OnboardingTermsScreen
import com.afternote.feature.onboarding.presentation.terms.TermsDetailScreen

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
                // #381: "시작하기" 도 "로그인하기" 와 동일하게 로그인 화면으로 이동 (PM 요청).
                // 신규 회원가입 진입은 로그인 화면 내 회원가입 링크(replaceLoginWithSignUp)로 유지.
                onStartClick = actions::navigateToLogin,
                onCheckRecordsClick = actions::navigateToReceivedRecords,
                onLoginClick = actions::navigateToLogin,
            )
        }

        // ── Login ──
        composable<OnboardingRoute.LoginRoute> {
            LoginScreen(
                onLoginSuccess = actions::replaceOnboardingWithHome,
                onNewUserOnboarding = actions::replaceLoginWithWelcome,
                onSignUpClick = actions::replaceLoginWithSignUp,
                onFindAccountClick = actions::navigateToFindId,
                onBackClick = actions::popBack,
            )
        }

        // ── 아이디 찾기 (인증 → 결과) ──
        composable<OnboardingRoute.FindIdRoute> {
            FindIdScreen(
                viewModel = graphScopedFindIdViewModel(graphScopedParentEntry),
                // 결과 화면 연결은 아이디 찾기 존치 결정(#456 코멘트) 대기 — 확정 전까지 미배선.
                onNextClick = {},
                onBackClick = actions::popBack,
            )
        }

        // ── SignUp Step 1: 이메일 & 인증번호 ──
        composable<OnboardingRoute.SignUpRoute> {
            SignUpScreen(
                viewModel = graphScopedSignUpViewModel(graphScopedParentEntry),
                onNavigateToResidentNumber = actions::proceedToSignUpResidentNumber,
                onBackClick = actions::popBack,
            )
        }

        // ── SignUp Step 2: 주민등록번호 ──
        composable<OnboardingRoute.SignUpResidentNumberRoute> {
            SignUpResidentNumberScreen(
                viewModel = graphScopedSignUpViewModel(graphScopedParentEntry),
                onNextClick = actions::proceedToSignUpPassword,
                onBackClick = actions::popBack,
            )
        }

        // ── SignUp Step 3: 비밀번호 설정 ──
        composable<OnboardingRoute.SignUpPasswordRoute> {
            SignUpPasswordScreen(
                viewModel = graphScopedSignUpViewModel(graphScopedParentEntry),
                onNextClick = actions::proceedToTerms,
                onBackClick = actions::popBack,
            )
        }

        // ── SignUp Step 4: 약관 동의 ──
        composable<OnboardingRoute.TermsRoute> {
            OnboardingTermsScreen(
                viewModel = graphScopedSignUpViewModel(graphScopedParentEntry),
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
            OnboardingProfileScreen(
                viewModel = graphScopedSignUpViewModel(graphScopedParentEntry),
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
 * `Route.Onboarding` 그래프 스코프에 묶인 [FindIdViewModel]을 가져옵니다.
 *
 * [NavBackStackEntry] 는 ViewModelStoreOwner 라서, [hiltViewModel] 에 부모 그래프 엔트리를 넘기면
 * VM 을 "만드는" 게 아니라 **그 엔트리의 store 에서 조회**한다 — 최초 호출만 생성이고, 같은 엔트리를
 * 넘기는 모든 화면이 동일 인스턴스를 받는다(인자를 생략하면 현재 화면 엔트리 스코프라 화면 pop 과 함께
 * 소멸). 수명은 그래프가 백스택에서 내려갈 때까지: 화면을 나갔다 재진입해도 상태가 남는 것은
 * [graphScopedSignUpViewModel] 과 동일 특성이다. 지금은 인증 화면 단독 소비지만 결과 화면(#474)·
 * 비밀번호 찾기(#457)가 [FindIdUiState.foundAccount] 등을 이어받는 통로로 이 스코프를 쓴다.
 * 람다를 [remember] 로 감싸는 건 getBackStackEntry 조회를 컴포지션당 1회로 고정하기 위함.
 */
@Composable
private fun graphScopedFindIdViewModel(graphScopedParentEntry: () -> NavBackStackEntry): FindIdViewModel {
    val parentEntry = remember { graphScopedParentEntry() }
    return hiltViewModel(parentEntry)
}
