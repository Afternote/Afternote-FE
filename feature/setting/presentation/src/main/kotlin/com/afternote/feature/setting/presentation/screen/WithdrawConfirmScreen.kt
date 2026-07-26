package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.SettingUiState
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel

private const val WITHDRAW_CONFIRM_TEXT = "탈퇴하겠습니다"

@Composable
fun WithdrawConfirmScreen(
    uiState: SettingUiState,
    onBackClick: () -> Unit,
    onWithdrawSuccess: () -> Unit,
    viewModel: SettingViewModel,
    modifier: Modifier = Modifier,
) {
    val userName = (uiState as? SettingUiState.Success)?.name.orEmpty()
    val userEmail = (uiState as? SettingUiState.Success)?.email.orEmpty()
    val textState = rememberTextFieldState()
    var showError by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    val withdrawCompleted by viewModel.withdrawCompleted.collectAsStateWithLifecycle()
    val currentOnWithdrawSuccess by rememberUpdatedState(onWithdrawSuccess)

    LaunchedEffect(withdrawCompleted) {
        if (withdrawCompleted) currentOnWithdrawSuccess()
    }

    if (showCompletionDialog) {
        Popup(
            type = PopupType.Default,
            message = stringResource(R.string.withdraw_complete_message),
            confirmText = stringResource(R.string.withdraw_complete_button),
            onConfirm = viewModel::deleteAccount,
            onDismiss = {},
        )
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = "회원 탈퇴 안내",
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.withdraw_confirm_top_message),
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray9,
            )
            Spacer(Modifier.height(24.dp))
            WithdrawAccountSection(userName = userName, userEmail = userEmail)
            Spacer(Modifier.height(24.dp))
            AfternoteTextField(
                state = textState,
                placeholder = stringResource(R.string.withdraw_confirm_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )
            if (showError) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.withdraw_confirm_error),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.error,
                )
            }
            Spacer(Modifier.weight(1f))
            WithdrawConfirmBottomButtons(
                onBackClick = onBackClick,
                onWithdrawClick = {
                    if (textState.text.toString() == WITHDRAW_CONFIRM_TEXT) {
                        showCompletionDialog = true
                    } else {
                        showError = true
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WithdrawAccountSection(
    userName: String,
    userEmail: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color = AfternoteDesign.colors.gray2)
                .padding(start = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.withdraw_account_section),
            style = AfternoteDesign.typography.textField,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text(
                text = userName,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
            )
            Text(
                text = " ($userEmail)",
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun WithdrawConfirmBottomButtons(
    onBackClick: () -> Unit,
    onWithdrawClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AfternoteButton(
            text = stringResource(R.string.withdraw_confirm_prev_button),
            onClick = onBackClick,
            type = AfternoteButtonType.Plain,
            modifier = Modifier.fillMaxWidth(),
        )
        AfternoteButton(
            text = stringResource(R.string.withdraw_confirm_button),
            onClick = onWithdrawClick,
            type = AfternoteButtonType.Default,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
