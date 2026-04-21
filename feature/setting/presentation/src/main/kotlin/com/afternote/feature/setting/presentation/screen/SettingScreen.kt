package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.SettingMenuItem
import com.afternote.feature.setting.presentation.component.SettingProfile
import com.afternote.feature.setting.presentation.viewmodel.SettingUiState
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel

@Composable
fun SettingScreen(
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
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val logoutCompleted by viewModel.logoutCompleted.collectAsStateWithLifecycle()
    val currentOnLogoutSuccess by rememberUpdatedState(onLogoutSuccess)

    LaunchedEffect(logoutCompleted) {
        if (logoutCompleted) {
            currentOnLogoutSuccess()
        }
    }

    SettingScreenContent(
        uiState = uiState,
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
        modifier = modifier,
    )
}

@Composable
private fun SettingScreenContent(
    uiState: SettingUiState,
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
    modifier: Modifier = Modifier,
) {
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
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                SettingProfile(name = state.name, email = state.email)

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
                    onClick = onLogoutClick,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        is SettingUiState.Error -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SettingMenuItem(
                    label = stringResource(R.string.settings_logout),
                    onClick = onLogoutClick,
                )
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingScreenPrev() {
    SettingScreenContent(
        uiState = SettingUiState.Success(name = "박서연", email = "afternote@email.com"),
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
    )
}
