package com.afternote.feature.onboarding.presentation.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.asString
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.onboarding.presentation.findaccount.FindPasswordCompleteScreen
import com.afternote.feature.onboarding.presentation.findaccount.FindPasswordResetScreen
import com.afternote.feature.onboarding.presentation.findaccount.FindPasswordScreen
import com.afternote.feature.onboarding.presentation.findaccount.FindPasswordUiState
import com.afternote.feature.onboarding.presentation.findaccount.FindPasswordViewModel

/**
 * 비밀번호 찾기(이메일 인증 → 새 비밀번호 → 완료)의 **흐름 전용 로컬 스택** (#457 · #1789).
 *
 * 세 화면이 [FindPasswordViewModel] 하나를 공유한다. 온보딩 스택에 셋을 나란히 두면 그
 * ViewModel 을 걸 자리가 host 밖에 없어 온보딩 전체와 수명이 같아지므로, 흐름 키
 * ([OnboardingRoute.FindPasswordFlowRoute]) 를 한 칸 두고 그 안에서 다시 스택을 연다 —
 * 흐름 entry 범위라 «세 화면 사이 공유, 흐름 이탈 시 정리» 가 된다.
 *
 * 인증번호는 서버가 이미 소비했으므로 각 단계를 지나면 앞 화면을 남기지 않는다.
 *
 * @param onExitFlow 흐름 바닥에서의 back — 바깥 온보딩 스택이 이 흐름 entry 를 내린다.
 * @param onLoginAfterReset 완료의 "로그인" — 바깥 스택을 로그인 하나로 수렴시킨다.
 */
@Composable
internal fun FindPasswordFlowHost(
    onExitFlow: () -> Unit,
    onLoginAfterReset: () -> Unit,
) {
    val viewModel: FindPasswordViewModel = hiltViewModel()
    val stepStack = rememberNavBackStack(OnboardingRoute.FindPasswordRoute)
    val boundary = remember(onExitFlow) { FeatureStackBoundary(onExitFlow) }
    val actions =
        remember(stepStack, boundary, onLoginAfterReset) {
            FindPasswordFlowLocalNavActions(stepStack, boundary, onLoginAfterReset)
        }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberFindPasswordEventHost(viewModel, uiState, actions::proceedToComplete)

    FeatureNavDisplay(
        backStack = stepStack,
        boundary = boundary,
        entryProvider =
            entryProvider {
                entry<OnboardingRoute.FindPasswordRoute> {
                    FindPasswordScreen(
                        initialEmail = uiState.email,
                        initialCertificateCode = uiState.certificateCode,
                        isSendingCode = uiState.isSendingCode,
                        isVerificationSent = uiState.isVerificationSent,
                        isSendCodeEnabled = uiState.isSendCodeEnabled,
                        isNextEnabled = uiState.isVerificationNextEnabled,
                        resendCooldownSeconds = uiState.resendCooldownSeconds,
                        showSocialAccountBlockedPopup = uiState.isSocialSignUpAccount,
                        snackbarHostState = snackbarHostState,
                        onEmailChange = viewModel::updateEmail,
                        onCertificateCodeChange = viewModel::updateCertificateCode,
                        onRequestCode = viewModel::requestVerificationCode,
                        onSocialAccountBlockedConfirm = viewModel::onSocialAccountBlockedConsumed,
                        onNextClick = actions::proceedToPasswordReset,
                        onBackClick = actions::popBack,
                    )
                }

                entry<OnboardingRoute.FindPasswordResetRoute> {
                    FindPasswordResetScreen(
                        initialPassword = uiState.newPassword,
                        initialPasswordConfirm = uiState.newPasswordConfirm,
                        isPasswordRuleSatisfied = uiState.isPasswordRuleSatisfied,
                        isNextEnabled = uiState.isResetEnabled,
                        snackbarHostState = snackbarHostState,
                        onPasswordChange = viewModel::updateNewPassword,
                        onPasswordConfirmChange = viewModel::updateNewPasswordConfirm,
                        onNextClick = viewModel::submitNewPassword,
                        onBackClick = actions::popBack,
                    )
                }

                entry<OnboardingRoute.FindPasswordCompleteRoute> {
                    FindPasswordCompleteScreen(onLoginClick = actions::popToLogin)
                }
            },
    )
}

/**
 * 비밀번호 찾기 흐름의 Snackbar 호스트 + 단발성 신호 처리.
 *
 * 비밀번호 변경 성공([FindPasswordUiState.isPasswordChanged])은 «완료 화면으로 이동» 이라는
 * 화면 전환 신호라 여기서 소비한다 — 이동 후 [FindPasswordViewModel.onPasswordResetConsumed] 로
 * 신호를 내려 재진입 시 다시 튀지 않게 한다.
 */
@Composable
private fun rememberFindPasswordEventHost(
    viewModel: FindPasswordViewModel,
    uiState: FindPasswordUiState,
    onPasswordChanged: () -> Unit,
): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isPasswordChanged) {
        if (uiState.isPasswordChanged) {
            onPasswordChanged()
            viewModel.onPasswordResetConsumed()
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
