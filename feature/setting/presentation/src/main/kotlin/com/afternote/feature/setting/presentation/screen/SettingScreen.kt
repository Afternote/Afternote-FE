package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.SettingLoadErrorContent
import com.afternote.feature.setting.presentation.component.SettingMenuItem
import com.afternote.feature.setting.presentation.component.SettingProfile
import com.afternote.feature.setting.presentation.component.SettingSection
import com.afternote.feature.setting.presentation.viewmodel.SettingUiState
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel

// 설정-메인
@Composable
fun SettingScreen(
    onBackClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onProfileEditClick: () -> Unit,
    onPasswordChangeClick: () -> Unit,
    onLinkedAccountClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onRecipientListClick: () -> Unit,
    onRecipientRegisterClick: () -> Unit,
    onAfterDeliveryClick: () -> Unit,
    onPasskeyClick: () -> Unit,
    onAppLockClick: () -> Unit,
    onFaqClick: () -> Unit,
    onInquiryClick: () -> Unit,
    onNoticeClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onServiceInfoClick: () -> Unit,
    onWithdrawGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val logoutCompleted by viewModel.logoutCompleted.collectAsStateWithLifecycle()
    val currentOnLogoutSuccess by rememberUpdatedState(onLogoutSuccess)

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(logoutCompleted) {
        if (logoutCompleted) {
            currentOnLogoutSuccess()
        }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.settings_title),
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
        containerColor = Color.Transparent,
    ) { innerPadding ->
        SettingScreenContent(
            uiState = uiState,
            onRetry = viewModel::refresh,
            onLogoutClick = viewModel::logout,
            onProfileEditClick = onProfileEditClick,
            onPasswordChangeClick = onPasswordChangeClick,
            onLinkedAccountClick = onLinkedAccountClick,
            onNotificationClick = onNotificationClick,
            onRecipientListClick = onRecipientListClick,
            onRecipientRegisterClick = onRecipientRegisterClick,
            onAfterDeliveryClick = onAfterDeliveryClick,
            onPasskeyClick = onPasskeyClick,
            onAppLockClick = onAppLockClick,
            onFaqClick = onFaqClick,
            onInquiryClick = onInquiryClick,
            onNoticeClick = onNoticeClick,
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick,
            onServiceInfoClick = onServiceInfoClick,
            onWithdrawGuideClick = onWithdrawGuideClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun SettingScreenContent(
    uiState: SettingUiState,
    onRetry: () -> Unit,
    onLogoutClick: () -> Unit,
    onProfileEditClick: () -> Unit,
    onPasswordChangeClick: () -> Unit,
    onLinkedAccountClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onRecipientListClick: () -> Unit,
    onRecipientRegisterClick: () -> Unit,
    onAfterDeliveryClick: () -> Unit,
    onPasskeyClick: () -> Unit,
    onAppLockClick: () -> Unit,
    onFaqClick: () -> Unit,
    onInquiryClick: () -> Unit,
    onNoticeClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onServiceInfoClick: () -> Unit,
    onWithdrawGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        Popup(
            type = PopupType.Variant2,
            message = stringResource(R.string.settings_logout_dialog_message),
            confirmText = stringResource(R.string.settings_logout_dialog_confirm),
            dismissText = stringResource(R.string.settings_logout_dialog_cancel),
            onConfirm = {
                showLogoutDialog = false
                onLogoutClick()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }

    when (val state = uiState) {
        is SettingUiState.Loading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        }

        is SettingUiState.Success -> {
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            ) {
                SettingProfile(
                    name = state.name,
                    email = state.email,
                    onNoticeClick = onNoticeClick,
                    onRecipientListClick = onRecipientListClick,
                )

                SettingSection(title = stringResource(R.string.settings_section_account)) {
                    SettingMenuItem(
                        label = stringResource(R.string.settings_account_profile_edit),
                        onClick = onProfileEditClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_account_password_change),
                        onClick = onPasswordChangeClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_account_linked_account),
                        onClick = onLinkedAccountClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_account_notification),
                        onClick = onNotificationClick,
                    )
                }

                SettingSection(title = stringResource(R.string.settings_section_recipient)) {
                    SettingMenuItem(
                        label = stringResource(R.string.settings_recipient_list),
                        onClick = onRecipientListClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_recipient_register),
                        onClick = onRecipientRegisterClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_recipient_after_delivery),
                        onClick = onAfterDeliveryClick,
                    )
                }

                SettingSection(title = stringResource(R.string.settings_section_security)) {
                    SettingMenuItem(
                        label = stringResource(R.string.settings_security_passkey),
                        onClick = onPasskeyClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_security_app_lock),
                        onClick = onAppLockClick,
                    )
                }

                SettingSection(title = stringResource(R.string.settings_section_support)) {
                    SettingMenuItem(
                        label = stringResource(R.string.settings_support_faq),
                        onClick = onFaqClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_support_inquiry),
                        onClick = onInquiryClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_support_notice),
                        onClick = onNoticeClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_support_terms),
                        onClick = onTermsClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_support_privacy),
                        onClick = onPrivacyClick,
                    )
                    SettingMenuItem(
                        label = stringResource(R.string.settings_support_service_info),
                        onClick = onServiceInfoClick,
                    )
                }

                SettingMenuItem(
                    label = stringResource(R.string.settings_logout),
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.padding(top = 8.dp),
                )
                SettingMenuItem(
                    label = stringResource(R.string.settings_account_withdraw),
                    onClick = onWithdrawGuideClick,
                )
            }
        }

        is SettingUiState.Error -> {
            SettingLoadErrorContent(
                message = state.message,
                onRetry = onRetry,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingScreenPrev() {
    Scaffold(
        topBar = { DetailTopBar(title = "설정") },
    ) { innerPadding ->
        SettingScreenContent(
            uiState = SettingUiState.Success(name = "박서연", email = "afternote@email.com"),
            onRetry = {},
            onLogoutClick = {},
            onProfileEditClick = {},
            onPasswordChangeClick = {},
            onLinkedAccountClick = {},
            onNotificationClick = {},
            onRecipientListClick = {},
            onRecipientRegisterClick = {},
            onAfterDeliveryClick = {},
            onPasskeyClick = {},
            onAppLockClick = {},
            onFaqClick = {},
            onInquiryClick = {},
            onNoticeClick = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onServiceInfoClick = {},
            onWithdrawGuideClick = {},
            modifier = Modifier.padding(innerPadding),
        )
    }
}
