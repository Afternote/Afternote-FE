package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.StepProgressBar
import com.afternote.core.ui.addFocusCleaner
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R

internal const val SIGN_UP_TOTAL_STEPS = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    currentStep: Int,
    emailState: TextFieldState,
    passwordState: TextFieldState,
    isVerificationSent: Boolean,
    onRequestVerification: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val progressDescription =
        stringResource(R.string.signup_progress_description, currentStep, SIGN_UP_TOTAL_STEPS)

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.signup_title),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        SignUpContent(
            currentStep = currentStep,
            emailState = emailState,
            passwordState = passwordState,
            isVerificationSent = isVerificationSent,
            onRequestVerification = onRequestVerification,
            onNextClick = onNextClick,
            progressDescription = progressDescription,
            modifier =
                Modifier
                    .padding(top = innerPadding.calculateTopPadding())
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
        )
    }
}

@Composable
private fun SignUpContent(
    currentStep: Int,
    emailState: TextFieldState,
    passwordState: TextFieldState,
    isVerificationSent: Boolean,
    onRequestVerification: () -> Unit,
    onNextClick: () -> Unit,
    progressDescription: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier =
            modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 49.dp)
                .addFocusCleaner(focusManager),
    ) {
        StepProgressBar(
            currentStep = currentStep,
            totalSteps = SIGN_UP_TOTAL_STEPS,
            contentDescription = progressDescription,
        )
        Spacer(Modifier.height(35.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 이메일 입력 + 인증번호 받기
            AfternoteTextField(
                state = emailState,
                placeholder = stringResource(R.string.signup_email_placeholder),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                trailingContent = {
                    Text(
                        text = stringResource(com.afternote.core.ui.R.string.core_ui_request_verification_code),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    focusManager.clearFocus()
                                    onRequestVerification()
                                }.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray9,
                    )
                },
            )

            // 비밀번호 입력
            AfternoteTextField(
                state = passwordState,
                placeholder = stringResource(R.string.signup_password_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            )

            // 인증번호 전송 안내 메시지
            if (isVerificationSent) {
                Text(
                    text = stringResource(R.string.signup_verification_sent),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = AfternoteDesign.colors.b1,
                )
            }
            Spacer(Modifier.weight(1f))
            // 다음 버튼
            AfternoteButton(
                text = stringResource(R.string.signup_next),
                onClick = {
                    focusManager.clearFocus()
                    onNextClick()
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    AfternoteTheme {
        SignUpScreen(
            currentStep = 1,
            emailState = rememberTextFieldState(),
            passwordState = rememberTextFieldState(),
            isVerificationSent = true,
            onRequestVerification = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
