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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R

@Composable
fun SignUpScreen(
    initialEmail: String,
    initialVerificationCode: String,
    isVerificationSent: Boolean,
    isSendingCode: Boolean,
    isEmailFormatValid: Boolean,
    resendCooldownSeconds: Int,
    hasVerificationError: Boolean,
    isNextEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onEmailChange: (String) -> Unit,
    onVerificationCodeChange: (String) -> Unit,
    onRequestVerification: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emailState = rememberTextFieldState(initialEmail)
    val verificationCodeState = rememberTextFieldState(initialVerificationCode)

    LaunchedEffect(emailState) {
        snapshotFlow { emailState.text.toString() }.collect(onEmailChange)
    }
    LaunchedEffect(verificationCodeState) {
        snapshotFlow { verificationCodeState.text.toString() }.collect(onVerificationCodeChange)
    }

    val verificationButtonText =
        when {
            isSendingCode -> {
                stringResource(R.string.onboarding_signup_verification_requesting)
            }

            resendCooldownSeconds > 0 -> {
                stringResource(R.string.onboarding_signup_verification_resend_cooldown, resendCooldownSeconds)
            }

            isVerificationSent -> {
                stringResource(R.string.onboarding_signup_verification_resend)
            }

            else -> {
                stringResource(R.string.onboarding_signup_verification_request)
            }
        }
    val isVerificationButtonEnabled =
        !isSendingCode && resendCooldownSeconds == 0 && isEmailFormatValid

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.onboarding_signup_title),
        actionButtonText = stringResource(R.string.onboarding_signup_next),
        onBackClick = onBackClick,
        onActionClick = onNextClick,
        modifier = modifier,
        isActionEnabled = isNextEnabled,
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
                            onClick = onRequestVerification,
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
                        if (isNextEnabled) onNextClick()
                    },
                )

                // 인증번호 불일치는 인라인 에러로, 그 외 실패는 스낵바로 나뉜다 (시안 2431-14204).
                if (hasVerificationError) {
                    Text(
                        text = stringResource(R.string.onboarding_signup_verification_mismatch),
                        style = AfternoteDesign.typography.captionLargeB,
                        color = AfternoteDesign.colors.error,
                    )
                } else if (isVerificationSent) {
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

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    AfternoteTheme {
        SignUpScreen(
            initialEmail = "",
            initialVerificationCode = "",
            isVerificationSent = true,
            isSendingCode = false,
            isEmailFormatValid = false,
            resendCooldownSeconds = 0,
            hasVerificationError = false,
            isNextEnabled = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onVerificationCodeChange = {},
            onRequestVerification = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
