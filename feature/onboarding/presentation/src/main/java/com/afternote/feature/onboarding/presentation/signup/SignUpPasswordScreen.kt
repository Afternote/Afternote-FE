package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.PasswordMaskTransformation
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R

private const val PASSWORD_MIN_LENGTH = 8
private const val PASSWORD_MAX_LENGTH = 16

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpPasswordScreen(
    currentStep: Int,
    passwordState: TextFieldState,
    passwordConfirmState: TextFieldState,
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
        SignUpPasswordContent(
            currentStep = currentStep,
            passwordState = passwordState,
            passwordConfirmState = passwordConfirmState,
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
private fun SignUpPasswordContent(
    currentStep: Int,
    passwordState: TextFieldState,
    passwordConfirmState: TextFieldState,
    onNextClick: () -> Unit,
    progressDescription: String,
    modifier: Modifier = Modifier,
) {
    val isNextEnabled by remember {
        derivedStateOf {
            passwordState.text.length in PASSWORD_MIN_LENGTH..PASSWORD_MAX_LENGTH &&
                passwordState.text.contentEquals(passwordConfirmState.text)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
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
            Text(
                text = stringResource(R.string.signup_password_input_label),
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 비밀번호 입력
            AfternoteTextField(
                state = passwordState,
                placeholder = stringResource(R.string.signup_password_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                outputTransformation = PasswordMaskTransformation,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 비밀번호 확인
            AfternoteTextField(
                state = passwordConfirmState,
                placeholder = stringResource(R.string.signup_password_confirm_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                outputTransformation = PasswordMaskTransformation,
                onImeAction = {
                    if (isNextEnabled) {
                        onNextClick()
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 안내 문구
            PasswordRuleItem(
                text = stringResource(R.string.signup_password_rule_combination),
                textColor = AfternoteDesign.colors.b1,
            )
            Spacer(modifier = Modifier.height(4.dp))
            PasswordRuleItem(
                text = stringResource(R.string.signup_password_rule_reuse),
                textColor = AfternoteDesign.colors.b1,
            )
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
            type =
                if (isNextEnabled) {
                    AfternoteButtonType.Default
                } else {
                    AfternoteButtonType.Un
                },
        )
    }
}

@Composable
private fun PasswordRuleItem(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "\u2022",
            modifier = Modifier.clearAndSetSemantics {},
            style =
                AfternoteDesign.typography.captionLargeR.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
            color = textColor,
        )
        Text(
            text = text,
            style =
                AfternoteDesign.typography.captionLargeR.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
            color = textColor,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpPasswordScreenPreview() {
    AfternoteTheme {
        SignUpPasswordContent(
            currentStep = 3,
            passwordState = rememberTextFieldState(),
            passwordConfirmState = rememberTextFieldState(),
            onNextClick = {},
            progressDescription = "회원가입 진행도 4단계 중 3단계",
        )
    }
}
