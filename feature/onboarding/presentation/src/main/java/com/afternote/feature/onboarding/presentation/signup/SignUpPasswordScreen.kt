package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.feature.onboarding.presentation.PasswordRuleItem
import com.afternote.feature.onboarding.presentation.R

@Composable
fun SignUpPasswordScreen(
    initialPassword: String,
    initialPasswordConfirm: String,
    isPasswordRuleSatisfied: Boolean,
    isNextEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val passwordState = rememberTextFieldState(initialPassword)
    val passwordConfirmState = rememberTextFieldState(initialPasswordConfirm)

    LaunchedEffect(passwordState) {
        snapshotFlow { passwordState.text.toString() }.collect(onPasswordChange)
    }
    LaunchedEffect(passwordConfirmState) {
        snapshotFlow { passwordConfirmState.text.toString() }.collect(onPasswordConfirmChange)
    }

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.onboarding_signup_title),
        actionButtonText = stringResource(R.string.onboarding_signup_next),
        onBackClick = onBackClick,
        onActionClick = onNextClick,
        modifier = modifier,
        isActionEnabled = isNextEnabled,
        currentStep = SignUpStep.PASSWORD,
        totalSteps = SIGN_UP_TOTAL_STEPS,
        progressContentDescription = stringResource(R.string.onboarding_step_description, SignUpStep.PASSWORD),
        snackbarHostState = snackbarHostState,
        content = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 43.dp),
            ) {
                SignUpInputLabel(
                    text = stringResource(R.string.onboarding_signup_password_input_label),
                )

                Spacer(modifier = Modifier.height(17.dp))

                // 비밀번호 입력
                AfternoteTextField(
                    state = passwordState,
                    placeholder = stringResource(R.string.onboarding_signup_password_placeholder),
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 비밀번호 확인
                AfternoteTextField(
                    state = passwordConfirmState,
                    placeholder = stringResource(R.string.onboarding_signup_password_confirm_placeholder),
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        if (isNextEnabled) onNextClick()
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 안내 문구
                PasswordRuleItem(
                    text = stringResource(R.string.onboarding_signup_password_rule_combination),
                    isSatisfied = isPasswordRuleSatisfied,
                )
                Spacer(modifier = Modifier.height(4.dp))
                PasswordRuleItem(
                    text = stringResource(R.string.onboarding_signup_password_rule_reuse),
                )
            }
        },
    )
}
