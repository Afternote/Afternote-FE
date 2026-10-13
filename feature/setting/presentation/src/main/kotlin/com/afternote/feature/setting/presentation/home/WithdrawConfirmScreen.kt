package com.afternote.feature.setting.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.setting.presentation.R

@Composable
internal fun WithdrawConfirmScreen(
    uiState: SettingUiState,
    onBackClick: () -> Unit,
    onWithdrawSuccess: () -> Unit,
    viewModel: SettingViewModel,
    modifier: Modifier = Modifier,
) {
    val userName = (uiState.profile as? SettingProfileState.Success)?.name.orEmpty()
    val userEmail = (uiState.profile as? SettingProfileState.Success)?.email.orEmpty()
    val withdrawUiState = uiState.withdraw

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
                onConfirm = { viewModel.onIntent(SettingIntent.DeleteAccount) },
                onDismiss = { viewModel.onIntent(SettingIntent.DismissWithdrawError) },
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
        onWithdrawClick = { viewModel.onIntent(SettingIntent.DeleteAccount) },
        isLoading = withdrawUiState == WithdrawUiState.Loading,
        modifier = modifier,
    )
}
