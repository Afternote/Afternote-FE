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

private const val SECONDS_PER_MINUTE = 60

@Composable
fun SignUpScreen(
    initialEmail: String,
    initialVerificationCode: String,
    isVerificationSent: Boolean,
    isSendingCode: Boolean,
    isEmailFormatValid: Boolean,
    resendCooldownSeconds: Int,
    verificationRemainingSeconds: Int,
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
                stringResource(R.string.signup_verification_requesting)
            }

            resendCooldownSeconds > 0 -> {
                stringResource(R.string.signup_verification_resend_cooldown, resendCooldownSeconds)
            }

            isVerificationSent -> {
                stringResource(R.string.signup_verification_resend)
            }

            else -> {
                stringResource(R.string.signup_verification_request)
            }
        }
    val isVerificationButtonEnabled =
        !isSendingCode && resendCooldownSeconds == 0 && isEmailFormatValid

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.signup_title),
        actionButtonText = stringResource(R.string.signup_next),
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
                    placeholder = stringResource(R.string.signup_email_placeholder),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )

                // 인증번호 입력
                AfternoteTextField(
                    state = verificationCodeState,
                    placeholder = stringResource(R.string.signup_verification_code_placeholder),
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (isNextEnabled) onNextClick()
                    },
                )

                // 인증번호 전송 안내 — 발송 직후 남은 시간 카운트다운, 만료 시 다시 받기 안내.
                if (isVerificationSent) {
                    val isExpired = verificationRemainingSeconds == 0
                    val message =
                        if (isExpired) {
                            stringResource(R.string.signup_verification_expired)
                        } else {
                            stringResource(
                                R.string.signup_verification_sent_with_timer,
                                verificationRemainingSeconds / SECONDS_PER_MINUTE,
                                verificationRemainingSeconds % SECONDS_PER_MINUTE,
                            )
                        }
                    Text(
                        text = message,
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
            verificationRemainingSeconds = 172,
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

@Preview(showBackground = true, name = "인증번호 만료")
@Composable
private fun SignUpScreenExpiredPreview() {
    AfternoteTheme {
        SignUpScreen(
            initialEmail = "user@example.com",
            initialVerificationCode = "",
            isVerificationSent = true,
            isSendingCode = false,
            isEmailFormatValid = true,
            resendCooldownSeconds = 0,
            verificationRemainingSeconds = 0,
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
