package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.signup.component.SignUpStepScaffold

internal const val SIGN_UP_TOTAL_STEPS = 4

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

    SignUpStepScaffold(
        currentStep = currentStep,
        onBackClick = onBackClick,
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomButton = {
            AfternoteButton(
                text = stringResource(R.string.signup_next),
                onClick = {
                    focusManager.clearFocus()
                    onNextClick()
                },
                modifier = Modifier.padding(bottom = 49.dp),
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(top = 35.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 이메일 입력 + 인증번호 받기
            AfternoteTextField(
                type = TextFieldType.Variant7,
                state = emailState,
                placeholder = stringResource(R.string.signup_email_placeholder),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
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
