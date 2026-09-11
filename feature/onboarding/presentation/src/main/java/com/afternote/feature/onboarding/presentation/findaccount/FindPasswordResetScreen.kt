package com.afternote.feature.onboarding.presentation.findaccount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.onboarding.presentation.PasswordRuleItem
import com.afternote.feature.onboarding.presentation.R

private val HeaderSpacing = 8.dp

/**
 * 비밀번호 찾기 2단계 — 새 비밀번호 입력 (시안 `2383:16789`).
 *
 * 안내 2줄은 회원가입 3단계와 글자까지 같은 시안이라 문구·규칙·컴포넌트를 그대로 쓴다
 * ([PasswordRuleItem] · `onboarding_signup_password_rule_*`).
 *
 * "다음" 이 곧 최종 제출이다 — 이 화면의 제출이 인증번호 검증까지 겸한다
 * ([FindPasswordViewModel.submitNewPassword]).
 */
@Composable
fun FindPasswordResetScreen(
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
        topBarTitle = stringResource(R.string.onboarding_find_password_title),
        actionButtonText = stringResource(R.string.onboarding_find_account_next),
        onBackClick = onBackClick,
        onActionClick = onNextClick,
        modifier = modifier,
        isActionEnabled = isNextEnabled,
        snackbarHostState = snackbarHostState,
    ) {
        Spacer(modifier = Modifier.height(HeaderSpacing))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_find_password_reset_title),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.gray9,
            )
            Text(
                text = stringResource(R.string.onboarding_find_password_reset_description),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray5,
            )

            AfternoteTextField(
                state = passwordState,
                placeholder = stringResource(R.string.onboarding_signup_password_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            )

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

            Column {
                PasswordRuleItem(
                    text = stringResource(R.string.onboarding_signup_password_rule_combination),
                    isSatisfied = isPasswordRuleSatisfied,
                )
                Spacer(modifier = Modifier.height(4.dp))
                PasswordRuleItem(
                    text = stringResource(R.string.onboarding_signup_password_rule_reuse),
                )
            }
        }
    }
}
