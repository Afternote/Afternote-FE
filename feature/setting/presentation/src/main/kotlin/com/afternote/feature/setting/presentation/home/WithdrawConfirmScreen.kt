package com.afternote.feature.setting.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.setting.presentation.R

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
    val withdrawUiState by viewModel.withdrawUiState.collectAsStateWithLifecycle()

    when (withdrawUiState) {
        WithdrawUiState.Success -> {
            Popup(
                type = PopupType.Default,
                message = stringResource(R.string.setting_withdraw_complete_message),
                confirmText = stringResource(R.string.setting_withdraw_complete_button),
                onConfirm = onWithdrawSuccess,
                onDismiss = onWithdrawSuccess,
            )
        }

        WithdrawUiState.Error -> {
            Popup(
                type = PopupType.Variant2,
                message = stringResource(R.string.setting_withdraw_failed_message),
                confirmText = stringResource(R.string.setting_withdraw_retry_button),
                dismissText = stringResource(R.string.setting_withdraw_close_button),
                onConfirm = viewModel::deleteAccount,
                onDismiss = viewModel::dismissWithdrawError,
            )
        }

        WithdrawUiState.Idle,
        WithdrawUiState.Loading,
        -> {}
    }

    WithdrawConfirmContent(
        userName = userName,
        userEmail = userEmail,
        onBackClick = onBackClick,
        onWithdrawClick = viewModel::deleteAccount,
        isLoading = withdrawUiState == WithdrawUiState.Loading,
        modifier = modifier,
    )
}
