package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.PasswordMaskTransformation
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
    val progressDescription =
        stringResource(R.string.signup_progress_description, currentStep, SIGN_UP_TOTAL_STEPS)

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.signup_title),
                onBackClick = onBackClick,
            )
        },
        containerColor = AfternoteDesign.colors.white,
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
                    .padding(innerPadding)
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
    Column(
        modifier =
            modifier.fillMaxSize(),
    ) {
        // 진행바
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / SIGN_UP_TOTAL_STEPS },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .semantics { contentDescription = progressDescription },
            color = AfternoteDesign.colors.gray9,
            trackColor = AfternoteDesign.colors.gray3,
            strokeCap = StrokeCap.Square,
        )

        // 입력 폼 (스크롤 가능)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 16.dp),
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
                                .clickable { onRequestVerification() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray9,
                    )
                },
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 비밀번호 입력
            AfternoteTextField(
                state = passwordState,
                placeholder = stringResource(R.string.signup_password_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                outputTransformation = PasswordMaskTransformation,
            )

            // 인증번호 전송 안내 메시지
            if (isVerificationSent) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.signup_verification_sent),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.b1,
                )
            }
        }

        // 다음 버튼
        AfternoteButton(
            text = stringResource(R.string.signup_next),
            onClick = onNextClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .height(48.dp),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpScreenPreview() {
    AfternoteTheme {
        SignUpContent(
            currentStep = 1,
            emailState = rememberTextFieldState(),
            passwordState = rememberTextFieldState(),
            isVerificationSent = true,
            onRequestVerification = {},
            onNextClick = {},
            progressDescription = "회원가입 진행도 4단계 중 1단계",
        )
    }
}
