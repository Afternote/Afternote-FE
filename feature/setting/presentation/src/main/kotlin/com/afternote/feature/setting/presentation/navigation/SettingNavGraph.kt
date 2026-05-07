package com.afternote.feature.setting.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.feature.setting.presentation.screen.SettingScreen
import com.afternote.feature.setting.presentation.screen.WithdrawConfirmScreen
import com.afternote.feature.setting.presentation.screen.WithdrawGuideScreen
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel

fun NavGraphBuilder.settingNavGraph(
    graphScopedParentEntry: () -> NavBackStackEntry,
    actions: SettingNavActions,
) {
    navigation<Route.Setting>(startDestination = SettingRoute.SettingHomeRoute) {
        composable<SettingRoute.SettingHomeRoute> {
            SettingScreen(
                onBackClick = {},
                onLogoutSuccess = actions::onLogoutSuccess,
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
                onWithdrawGuideClick = actions::onNavigateToWithdrawGuide,
            )
        }

        composable<SettingRoute.WithdrawGuideRoute> {
            val parentEntry = remember(it) { graphScopedParentEntry() }
            val viewModel: SettingViewModel = hiltViewModel(parentEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            WithdrawGuideScreen(
                uiState = uiState,
                onBackClick = actions::onWithdrawGuideBack,
                onCancelClick = actions::onWithdrawGuideBack,
                onConfirmClick = actions::onNavigateToWithdrawConfirm,
            )
        }

        composable<SettingRoute.WithdrawConfirmRoute> {
            val parentEntry = remember(it) { graphScopedParentEntry() }
            val viewModel: SettingViewModel = hiltViewModel(parentEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            WithdrawConfirmScreen(
                uiState = uiState,
                onBackClick = actions::onWithdrawConfirmBack,
                onWithdrawSuccess = actions::onWithdrawSuccess,
            )
        }
    }
}
