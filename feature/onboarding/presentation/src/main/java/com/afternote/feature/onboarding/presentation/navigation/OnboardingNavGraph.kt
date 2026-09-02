package com.afternote.feature.onboarding.presentation.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.core.ui.asString
import com.afternote.feature.onboarding.presentation.OnboardingProfileEntry
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.WelcomeScreen
import com.afternote.feature.onboarding.presentation.findaccount.FindIdScreen
import com.afternote.feature.onboarding.presentation.findaccount.FindIdUiState
import com.afternote.feature.onboarding.presentation.findaccount.FindIdViewModel
import com.afternote.feature.onboarding.presentation.login.LoginEntry
import com.afternote.feature.onboarding.presentation.signup.SignUpPasswordScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpResidentNumberScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpUiState
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
            LoginEntry(
                onLoginSuccess = actions::replaceOnboardingWithHome,
                onNewUserOnboarding = actions::replaceLoginWithWelcome,
                onSignUpClick = actions::replaceLoginWithSignUp,
                onFindAccountClick = actions::navigateToFindId,
                onBackClick = actions::popBack,
            )
        }

        // ── 아이디 찾기 (인증 → 결과) ──
        //
        // 현재 진입점이 없다. 로그인의 "아이디/비밀번호 찾기" 는 #457 로 비밀번호 찾기를 향하게
        // 바꿨다 — 이 화면의 종착지인 결과 화면(#474)이 not planned 로 닫혀 여기로 보내면
        // "확인" 뒤에 갈 곳이 없기 때문이다. 화면·라우트 제거는 #943(카카오 단일화) 몫이라
        // 여기서 지우지 않고, 그때까지 등록만 남긴다.
        composable<OnboardingRoute.FindIdRoute> {
            val viewModel = graphScopedFindIdViewModel(graphScopedParentEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = rememberFindIdEventHost(viewModel, uiState)

            FindIdScreen(
                initialEmail = uiState.email,
                initialCertificateCode = uiState.certificateCode,
                isSendingCode = uiState.isSendingCode,
                isVerificationSent = uiState.isVerificationSent,
                isSendCodeEnabled = uiState.isSendCodeEnabled,
                isVerifyEnabled = uiState.isVerifyEnabled,
                isNextEnabled = uiState.isNextEnabled,
                resendCooldownSeconds = uiState.resendCooldownSeconds,
                hasVerificationError = uiState.hasVerificationError,
                snackbarHostState = snackbarHostState,
                onEmailChange = viewModel::updateEmail,
                onCertificateCodeChange = viewModel::updateCertificateCode,
                onRequestCode = viewModel::requestVerificationCode,
                onVerifyCode = viewModel::verifyCode,
                // 결과 화면 연결은 아이디 찾기 존치 결정(#456 코멘트) 대기 — 확정 전까지 미배선.
                onNextClick = {},
                onBackClick = actions::popBack,
            )
        }

        // ── SignUp Step 1: 이메일 & 인증번호 ──
        composable<OnboardingRoute.SignUpRoute> {
            val signUpViewModel = graphScopedSignUpViewModel(graphScopedParentEntry)
            val uiState by signUpViewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState =
                rememberSignUpEventHost(
                    viewModel = signUpViewModel,
                    onNavigateToResidentNumber = actions::proceedToSignUpResidentNumber,
                )

            SignUpScreen(
                initialEmail = uiState.email,
                initialVerificationCode = uiState.verificationCode,
                isVerificationSent = uiState.isVerificationSent,
                isSendingCode = uiState.isSendingCode,
                isEmailFormatValid = uiState.isEmailFormatValid,
                resendCooldownSeconds = uiState.resendCooldownSeconds,
                hasVerificationError = uiState.hasVerificationError,
                isNextEnabled = uiState.isStep1NextEnabled,
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
            val uiState by signUpViewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = rememberSignUpEventHost(signUpViewModel)

            SignUpResidentNumberScreen(
                initialFrontNumber = uiState.residentFrontNumber,
                initialBackNumber = uiState.residentBackNumber,
                isNextEnabled = uiState.isStep2NextEnabled,
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
            val uiState by signUpViewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = rememberSignUpEventHost(signUpViewModel)

            SignUpPasswordScreen(
                initialPassword = uiState.signUpPassword,
                initialPasswordConfirm = uiState.signUpPasswordConfirm,
                isPasswordRuleSatisfied = uiState.isPasswordRuleSatisfied,
                isNextEnabled = uiState.isStep3NextEnabled,
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
            val uiState by signUpViewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = rememberSignUpEventHost(signUpViewModel)

            OnboardingTermsScreen(
                termsState = uiState.termsState,
                isNextEnabled = uiState.isStep4NextEnabled,
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

/**
 * 아이디 찾기 화면의 Snackbar 호스트 + 단발성 에러 신호 처리.
 *
 * 인증번호 불일치는 시안상 인라인 문구라 여기서 다루지 않고([FindIdUiState.hasVerificationError]),
 * 그 외 실패([FindIdUiState.errorMessage])만 snackbar 로 노출한다.
 */
@Composable
private fun rememberFindIdEventHost(
    viewModel: FindIdViewModel,
    uiState: FindIdUiState,
): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }
    // VM 이 UiText 로 폴백까지 확정해 두므로 빈 문구가 도달하지 않는다.
    val pendingErrorMessage = uiState.errorMessage?.asString()

    LaunchedEffect(pendingErrorMessage) {
        if (pendingErrorMessage != null) {
            snackbarHostState.showSnackbar(
                message = pendingErrorMessage,
                duration = SnackbarDuration.Short,
            )
            viewModel.onErrorConsumed()
        }
    }
    return snackbarHostState
}

/**
 * SignUp Step 화면 공통의 Snackbar 호스트 + UI state 단발성 신호 처리.
 *
 * 각 Step composable 에서 호출해 [SignUpUiState.errorMessage] / [SignUpUiState.isNameRequired]
 * 를 일관되게 snackbar 로 노출하고, Step 1 의 경우 [onNavigateToResidentNumber] 콜백으로
 * [SignUpUiState.shouldNavigateToResidentNumber] = true 시점에 네비게이트한다.
 *
 * sealed Event Channel 대신 UiState 의 nullable/boolean 신호 + [LaunchedEffect] + on*Consumed 패턴으로 통일
 * (Google 공식 가이드 — ViewModel events should always result in a UI state update).
 */
@Composable
private fun rememberSignUpEventHost(
    viewModel: SignUpViewModel,
    onNavigateToResidentNumber: (() -> Unit)? = null,
): SnackbarHostState {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val nameRequiredMessage = stringResource(R.string.onboarding_signup_name_required)

    LaunchedEffect(uiState.shouldNavigateToResidentNumber) {
        if (uiState.shouldNavigateToResidentNumber && onNavigateToResidentNumber != null) {
            onNavigateToResidentNumber()
            viewModel.onResidentNumberNavigatedConsumed()
        }
    }

    LaunchedEffect(uiState.isNameRequired) {
        if (uiState.isNameRequired) {
            snackbarHostState.showSnackbar(
                message = nameRequiredMessage,
                duration = SnackbarDuration.Short,
            )
            viewModel.onNameRequiredConsumed()
        }
    }

    val pendingErrorMessage = uiState.errorMessage?.asString()
    LaunchedEffect(pendingErrorMessage) {
        if (pendingErrorMessage != null) {
            snackbarHostState.showSnackbar(
                message = pendingErrorMessage,
                duration = SnackbarDuration.Short,
            )
            viewModel.onErrorConsumed()
        }
    }

    return snackbarHostState
}
