package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.Label
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.scaffold.OnboardingStepScaffold

@Composable
fun SignUpPasswordScreen(
    passwordState: TextFieldState,
    passwordConfirmState: TextFieldState,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    OnboardingStepScaffold(
        currentStep = 3,
        buttonText = stringResource(R.string.signup_next),
        onBackClick = onBackClick,
        onNextClick = onNextClick,
        modifier = modifier,
        content = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 43.dp),
            ) {
                Label(
                    text = stringResource(R.string.signup_password_input_label),
                )

                Spacer(modifier = Modifier.height(17.dp))

                // 비밀번호 입력
                AfternoteTextField(
                    state = passwordState,
                    placeholder = stringResource(R.string.signup_password_placeholder),
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 비밀번호 확인
                AfternoteTextField(
                    state = passwordConfirmState,
                    placeholder = stringResource(R.string.signup_password_confirm_placeholder),
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        onNextClick()
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 안내 문구
                PasswordRuleItem(
                    text = stringResource(R.string.signup_password_rule_combination),
                )
                Spacer(modifier = Modifier.height(4.dp))
                PasswordRuleItem(
                    text = stringResource(R.string.signup_password_rule_reuse),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        },
    )
}

@Composable
private fun PasswordRuleItem(
    text: String,
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
                AfternoteDesign.typography.captionLargeB,
            color = AfternoteDesign.colors.b1,
        )
        Text(
            text = text,
            style =
                AfternoteDesign.typography.captionLargeB,
            color = AfternoteDesign.colors.b1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpPasswordScreenPreview() {
    AfternoteTheme {
        SignUpPasswordScreen(
            passwordState = rememberTextFieldState(),
            passwordConfirmState = rememberTextFieldState(),
            onNextClick = {},
            onBackClick = {},
        )
    }
}
