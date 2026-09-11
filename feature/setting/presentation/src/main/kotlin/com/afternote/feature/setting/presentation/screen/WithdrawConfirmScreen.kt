package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.SettingIntent
import com.afternote.feature.setting.presentation.viewmodel.SettingProfileState
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel
import com.afternote.feature.setting.presentation.viewmodel.WithdrawUiState

@Composable
internal fun WithdrawConfirmScreen(
    uiState: SettingProfileState,
    onBackClick: () -> Unit,
    onWithdrawSuccess: () -> Unit,
    viewModel: SettingViewModel,
    modifier: Modifier = Modifier,
) {
    val userName = (uiState as? SettingProfileState.Success)?.name.orEmpty()
    val userEmail = (uiState as? SettingProfileState.Success)?.email.orEmpty()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val withdrawUiState = state.withdraw

    when (withdrawUiState) {
        WithdrawUiState.Success -> {
            Popup(
                type = PopupType.Default,
                message = stringResource(R.string.withdraw_complete_message),
                confirmText = stringResource(R.string.withdraw_complete_button),
                onConfirm = onWithdrawSuccess,
                onDismiss = onWithdrawSuccess,
            )
        }

        WithdrawUiState.Error -> {
            Popup(
                type = PopupType.Variant2,
                message = stringResource(R.string.withdraw_failed_message),
                confirmText = stringResource(R.string.withdraw_retry_button),
                dismissText = stringResource(R.string.withdraw_close_button),
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
