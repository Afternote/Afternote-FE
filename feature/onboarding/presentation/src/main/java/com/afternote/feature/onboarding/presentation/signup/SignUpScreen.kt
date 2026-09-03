package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.mvi.ObserveFlag
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.R

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel,
    onNavigateToResidentNumber: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberSignUpSnackbarHost(state, viewModel::onIntent)

    ObserveFlag(
        raised = state.shouldNavigateToResidentNumber,
        consumed = SignUpIntent.ConsumeResidentNumberNavigation,
        onIntent = viewModel::onIntent,
        onRaised = onNavigateToResidentNumber,
    )

    SignUpContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

/** Step 1 — stateless 층. 프리뷰·screenshotTest·Robolectric 의 진입점이다. */
@Composable
internal fun SignUpContent(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emailState = rememberTextFieldState(state.email)
    val verificationCodeState = rememberTextFieldState(state.verificationCode)

    LaunchedEffect(emailState) {
        snapshotFlow { emailState.text.toString() }.collect { onIntent(SignUpIntent.UpdateEmail(it)) }
    }
    LaunchedEffect(verificationCodeState) {
        snapshotFlow { verificationCodeState.text.toString() }.collect { onIntent(SignUpIntent.UpdateVerificationCode(it)) }
    }

    val verificationButtonText =
        when {
            state.isSendingCode -> {
                stringResource(R.string.onboarding_signup_verification_requesting)
            }

            state.resendCooldownSeconds > 0 -> {
                stringResource(R.string.onboarding_signup_verification_resend_cooldown, state.resendCooldownSeconds)
            }

            state.isVerificationSent -> {
                stringResource(R.string.onboarding_signup_verification_resend)
            }

            else -> {
                stringResource(R.string.onboarding_signup_verification_request)
            }
        }
    val isVerificationButtonEnabled =
        !state.isSendingCode && state.resendCooldownSeconds == 0 && state.isEmailFormatValid

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.onboarding_signup_title),
        actionButtonText = stringResource(R.string.onboarding_signup_next),
        onBackClick = onBackClick,
        onActionClick = { onIntent(SignUpIntent.VerifyEmailAndProceed) },
        modifier = modifier,
        isActionEnabled = state.isStep1NextEnabled,
        currentStep = SignUpStep.EMAIL,
        totalSteps = SIGN_UP_TOTAL_STEPS,
        progressContentDescription = stringResource(R.string.onboarding_step_description, SignUpStep.EMAIL),
        snackbarHostState = snackbarHostState,
        content = {
            Column(
                modifier =
                    Modifier
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 35.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 이메일 입력 + 인증번호 받기
                AfternoteTextField(
                    type =
                        TextFieldType.Variant7(
                            text = verificationButtonText,
                            onClick = { onIntent(SignUpIntent.RequestVerification) },
                            enabled = isVerificationButtonEnabled,
                        ),
                    state = emailState,
                    placeholder = stringResource(R.string.onboarding_signup_email_placeholder),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )

                // 인증번호 입력
                AfternoteTextField(
                    state = verificationCodeState,
                    placeholder = stringResource(R.string.onboarding_signup_verification_code_placeholder),
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (state.isStep1NextEnabled) onIntent(SignUpIntent.VerifyEmailAndProceed)
                    },
                )

                // 인증번호 불일치는 인라인 에러로, 그 외 실패는 스낵바로 나뉜다 (시안 2431-14204).
                if (state.hasVerificationError) {
                    Text(
                        text = stringResource(R.string.onboarding_signup_verification_mismatch),
                        style = AfternoteDesign.typography.captionLargeB,
                        color = AfternoteDesign.colors.error,
                    )
                } else if (state.isVerificationSent) {
                    Text(
                        text = stringResource(R.string.onboarding_signup_verification_sent),
                        style = AfternoteDesign.typography.captionLargeB,
                        color = AfternoteDesign.colors.b1,
                    )
                }
            }
        },
    )
}
