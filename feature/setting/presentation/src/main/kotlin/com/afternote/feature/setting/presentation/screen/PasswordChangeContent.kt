package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.asString
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.PasswordChangeIntent
import com.afternote.feature.setting.presentation.viewmodel.PasswordChangeUiState

/**
 * 비밀번호 변경 화면의 상태 없는 본문.
 *
 * 시안 대조는 하지 않았다 — 이 화면 전용 시안을 열 수단이 없었다. 배치는 같은 저장소의 새 비밀번호
 * 입력 선례(회원가입 3단계·비밀번호 찾기)를 따랐고, 문구와 간격은 시안이 확인되면 그때 맞춘다.
 */
@Composable
internal fun PasswordChangeContent(
    uiState: PasswordChangeUiState,
    onIntent: (PasswordChangeIntent) -> Unit,
    onBackClick: () -> Unit,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentPasswordState = rememberTextFieldState()
    val newPasswordState = rememberTextFieldState()
    // LaunchedEffect 는 키가 그대로면 최초 람다를 계속 들고 있다. 재구성으로 새로 온 핸들러를 쓰도록
    // 최신 값을 읽는다 — 키에 핸들러를 넣으면 매 재구성마다 수집이 끊겼다 다시 붙는다.
    val currentOnIntent by rememberUpdatedState(onIntent)

    LaunchedEffect(currentPasswordState) {
        snapshotFlow { currentPasswordState.text.toString() }.collect { value ->
            currentOnIntent(PasswordChangeIntent.UpdateCurrentPassword(value))
        }
    }
    LaunchedEffect(newPasswordState) {
        snapshotFlow { newPasswordState.text.toString() }.collect { value ->
            currentOnIntent(PasswordChangeIntent.UpdateNewPassword(value))
        }
    }

    ObserveSignal(
        signal = uiState.changed,
        consumed = PasswordChangeIntent.ConsumeChanged,
        onIntent = onIntent,
    ) {
        onChanged()
    }

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.settings_account_password_change),
        actionButtonText = stringResource(R.string.settings_password_change_submit),
        onBackClick = onBackClick,
        onActionClick = { onIntent(PasswordChangeIntent.Submit) },
        modifier = modifier,
        isActionEnabled = uiState.isSubmitEnabled,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            PasswordFieldLabel(text = stringResource(R.string.settings_password_change_current_label))
            AfternoteTextField(
                state = currentPasswordState,
                placeholder = stringResource(R.string.settings_password_change_current_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordFieldLabel(text = stringResource(R.string.settings_password_change_new_label))
            AfternoteTextField(
                state = newPasswordState,
                placeholder = stringResource(R.string.settings_password_change_new_placeholder),
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = { onIntent(PasswordChangeIntent.Submit) },
            )

            Text(
                text = stringResource(R.string.settings_password_change_rule),
                modifier = Modifier.fillMaxWidth(),
                style = AfternoteDesign.typography.captionLargeB,
                color =
                    if (uiState.isNewPasswordRuleSatisfied) {
                        AfternoteDesign.colors.b1
                    } else {
                        AfternoteDesign.colors.gray5
                    },
            )

            uiState.errorMessage?.let { message ->
                Text(
                    text = message.asString(),
                    modifier = Modifier.fillMaxWidth(),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.error,
                )
            }
        }
    }
}

@Composable
private fun PasswordFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = AfternoteDesign.typography.captionLargeR,
        color = AfternoteDesign.colors.gray6,
    )
}
