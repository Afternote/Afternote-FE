package com.afternote.feature.onboarding.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.onboarding.presentation.OnboardingProfileEntry
import com.afternote.feature.onboarding.presentation.WelcomeScreen
import com.afternote.feature.onboarding.presentation.findaccount.FindIdScreen
import com.afternote.feature.onboarding.presentation.findaccount.FindIdViewModel
import com.afternote.feature.onboarding.presentation.login.LoginEntry
import com.afternote.feature.onboarding.presentation.signup.SignUpPasswordScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpResidentNumberScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpScreen
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel
import com.afternote.feature.onboarding.presentation.terms.OnboardingTermsScreen
import com.afternote.feature.onboarding.presentation.terms.TermsDetailScreen

/**
 * 온보딩 피처가 소유하는 로컬 Navigation 3 스택.
 *
 * 흐름: Welcome → Login / SignUp(1~4단계) → Profile → 완료(Home 이동)
 *
 * ## 공유 ViewModel 수명
 *
 * Nav2 에서 [SignUpViewModel]·[FindIdViewModel] 은 `Route.Onboarding` **그래프 엔트리**의
 * `ViewModelStore` 에 묶여 있었다(`getBackStackEntry<Route.Onboarding>()` + `hiltViewModel(parentEntry)`).
 * Nav3 엔 그래프라는 중간 계층이 없으므로, 같은 수명을 **host 자신의 스코프**로 옮긴다 — 이
 * 컴포저블은 온보딩을 담은 상위 엔트리 안에서 실행되니 여기서 만든 ViewModel 은 그 엔트리가
 * 백스택에서 내려갈 때 정리된다. 즉 화면 사이 재진입에는 살아남고 온보딩을 벗어나면 사라지는,
 * 이관 전과 같은 특성이다.
 *
 * 화면별 ViewModel 이 필요해지면 `entry { }` 안에서 [hiltViewModel] 을 부른다 — 그쪽은
 * entry 범위(= 그 화면이 pop 되면 정리)로 잡힌다.
 */
@Composable
public fun OnboardingNavHost(
    boundary: FeatureStackBoundary,
    externalActions: OnboardingExternalActions,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(OnboardingRoute.WelcomeRoute)
    val actions =
        remember(backStack, boundary, externalActions) {
            OnboardingLocalNavActions(backStack, boundary, externalActions)
        }

    // 온보딩 전체가 공유하는 ViewModel — 상세는 KDoc 참고.
    val signUpViewModel: SignUpViewModel = hiltViewModel()
    val findIdViewModel: FindIdViewModel = hiltViewModel()

    FeatureNavDisplay(
        backStack = backStack,
        boundary = boundary,
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<OnboardingRoute.WelcomeRoute> {
                    WelcomeScreen(
                        // #381: "시작하기" 도 "로그인하기" 와 동일하게 로그인 화면으로 이동 (PM 요청).
                        // 신규 회원가입 진입은 로그인 화면 내 회원가입 링크(replaceLoginWithSignUp)로 유지.
                        onStartClick = actions::navigateToLogin,
                        onCheckRecordsClick = actions::navigateToReceivedRecords,
                        onLoginClick = actions::navigateToLogin,
                    )
                }

                entry<OnboardingRoute.LoginRoute> {
                    LoginEntry(
                        onLoginSuccess = actions::replaceOnboardingWithHome,
                        onNewUserOnboarding = actions::replaceLoginWithWelcome,
                        onSignUpClick = actions::replaceLoginWithSignUp,
                        onFindAccountClick = actions::navigateToFindId,
                        onBackClick = actions::popBack,
                    )
                }

                // 현재 진입점이 없다. 로그인의 "아이디/비밀번호 찾기" 는 #457 로 비밀번호 찾기를
                // 향하게 바꿨다 — 이 화면의 종착지인 결과 화면(#474)이 not planned 로 닫혀 여기로
                // 보내면 "확인" 뒤에 갈 곳이 없기 때문이다. 화면·라우트 제거는 #943(카카오 단일화)
                // 몫이라 여기서 지우지 않고, 그때까지 등록만 남긴다.
                entry<OnboardingRoute.FindIdRoute> {
                    val uiState by findIdViewModel.uiState.collectAsStateWithLifecycle()
                    val snackbarHostState = rememberFindIdEventHost(findIdViewModel, uiState)

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
                        onEmailChange = findIdViewModel::updateEmail,
                        onCertificateCodeChange = findIdViewModel::updateCertificateCode,
                        onRequestCode = findIdViewModel::requestVerificationCode,
                        onVerifyCode = findIdViewModel::verifyCode,
                        // 결과 화면 연결은 아이디 찾기 존치 결정(#456 코멘트) 대기 — 확정 전까지 미배선.
                        onNextClick = {},
                        onBackClick = actions::popBack,
                    )
                }

                entry<OnboardingRoute.SignUpRoute> {
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

                entry<OnboardingRoute.SignUpResidentNumberRoute> {
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

                entry<OnboardingRoute.SignUpPasswordRoute> {
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

                entry<OnboardingRoute.TermsRoute> {
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

                entry<OnboardingRoute.TermsDetailRoute> {
                    TermsDetailScreen(
                        title = "",
                        onBackClick = actions::popBack,
                        onNextClick = actions::popBack,
                    )
                }

                entry<OnboardingRoute.ProfileRoute> {
                    OnboardingProfileEntry(
                        viewModel = signUpViewModel,
                        onOnboardingComplete = actions::replaceOnboardingWithHome,
                        onBackClick = actions::popBack,
                    )
                }
            },
    )
}
