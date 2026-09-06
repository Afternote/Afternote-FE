package com.afternote.feature.setting.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.afternote.core.ui.Route
import com.afternote.feature.setting.presentation.component.PinSetupStep
import com.afternote.feature.setting.presentation.screen.AppLockSetupScreen
import com.afternote.feature.setting.presentation.screen.ConnectedAccountsScreen
import com.afternote.feature.setting.presentation.screen.DeliveryConditionScreen
import com.afternote.feature.setting.presentation.screen.InquiryDetailScreen
import com.afternote.feature.setting.presentation.screen.InquiryListScreen
import com.afternote.feature.setting.presentation.screen.InquiryWriteScreen
import com.afternote.feature.setting.presentation.screen.NoticeListScreen
import com.afternote.feature.setting.presentation.screen.PassKeyListScreen
import com.afternote.feature.setting.presentation.screen.PassKeyMakingScreen
import com.afternote.feature.setting.presentation.screen.PassKeyPasswordScreen
import com.afternote.feature.setting.presentation.screen.PassKeyScreen
import com.afternote.feature.setting.presentation.screen.ProfileEditScreen
import com.afternote.feature.setting.presentation.screen.PushNotificationScreen
import com.afternote.feature.setting.presentation.screen.ReceiverEditScreen
import com.afternote.feature.setting.presentation.screen.ReceiverListScreen
import com.afternote.feature.setting.presentation.screen.ReceiverRegisterScreen
import com.afternote.feature.setting.presentation.screen.SettingScreen
import com.afternote.feature.setting.presentation.screen.WithdrawConfirmScreen
import com.afternote.feature.setting.presentation.screen.WithdrawGuideScreen
import com.afternote.feature.setting.presentation.viewmodel.PassKeyViewModel
import com.afternote.feature.setting.presentation.viewmodel.ReceiverListViewModel
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel

fun NavGraphBuilder.settingNavGraph(
    graphScopedParentEntry: () -> NavBackStackEntry,
    actions: SettingNavActions,
) {
    navigation<Route.Setting>(startDestination = SettingRoute.SettingHomeRoute) {
        composable<SettingRoute.SettingHomeRoute> {
            SettingScreen(
                onBackClick = actions::onSettingBack,
                onLogoutSuccess = actions::onLogoutSuccess,
                onProfileEditClick = actions::onNavigateToProfileEdit,
                onPasswordChangeClick = {},
                onLinkedAccountClick = actions::onNavigateToLinkedAccount,
                onNotificationClick = actions::onNavigateToNotification,
                onRecipientListClick = actions::onNavigateToRecipientList,
                onRecipientRegisterClick = actions::onNavigateToRecipientRegister,
                onAfterDeliveryClick = {
                    actions.onNavigateToRecipientListForDeliveryConditions()
                },
                onPasskeyClick = actions::onNavigateToPasskey,
                onAppLockClick = actions::onNavigateToAppLock,
                onFaqClick = {},
                onInquiryClick = actions::onNavigateToInquiry,
                onNoticeClick = actions::onNavigateToNotice,
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
                viewModel = viewModel,
            )
        }

        composable<SettingRoute.ProfileEditRoute> {
            ProfileEditScreen(
                onBackClick = actions::onProfileEditBack,
                onWithdrawGuideClick = actions::onNavigateToWithdrawGuide,
            )
        }

        composable<SettingRoute.LinkedAccountRoute> {
            ConnectedAccountsScreen(
                onBack = actions::onLinkedAccountBack,
            )
        }

        composable<SettingRoute.NotificationRoute> {
            PushNotificationScreen(
                onBack = actions::onNotificationBack,
            )
        }

        composable<SettingRoute.RecipientListRoute> {
            val route = it.toRoute<SettingRoute.RecipientListRoute>()
            val viewModel: ReceiverListViewModel = hiltViewModel()
            val receivers by viewModel.receivers.collectAsStateWithLifecycle()
            ReceiverListScreen(
                receivers = receivers,
                onBackClick = actions::onRecipientListBack,
                onConfirmClick = { receiver ->
                    if (route.selectForDeliveryConditions) {
                        actions.onNavigateToAfterDelivery(receiver.receiverId)
                    } else {
                        actions.onRecipientListBack()
                    }
                },
            )
        }

        composable<SettingRoute.RecipientRegisterRoute> {
            ReceiverRegisterScreen(
                onBackClick = actions::onRecipientRegisterBack,
                onRegisterSuccess = actions::onRecipientRegisterBack,
            )
        }

        composable<SettingRoute.RecipientEditRoute> {
            ReceiverEditScreen(
                onBackClick = actions::onRecipientEditBack,
                onEditSuccess = actions::onRecipientEditBack,
            )
        }

        composable<SettingRoute.AfterDeliveryRoute> {
            val route = it.toRoute<SettingRoute.AfterDeliveryRoute>()
            DeliveryConditionScreen(
                onBack = actions::onAfterDeliveryBack,
                onSaveSuccess = actions::onAfterDeliveryBack,
                onLastGreetingEditClick = {
                    actions.onNavigateToRecipientEdit(route.receiverId)
                },
            )
        }

        composable<SettingRoute.PasskeyRoute> {
            val viewModel: PassKeyViewModel = hiltViewModel()
            val isPasskeyRegistered by viewModel.isPasskeyRegistered.collectAsStateWithLifecycle()
            if (isPasskeyRegistered == true) {
                PassKeyListScreen(onBackClick = actions::onPasskeyBack)
            } else if (isPasskeyRegistered == false) {
                PassKeyScreen(
                    onBackClick = actions::onPasskeyBack,
                    onRegisterClick = actions::onNavigateToPasskeyMaking,
                )
            }
        }

        composable<SettingRoute.PasskeyMakingRoute> {
            PassKeyMakingScreen(
                onBackClick = actions::onPasskeyMakingBack,
                onPasswordAuthClick = actions::onNavigateToPasskeyPassword,
            )
        }

        composable<SettingRoute.PasskeyPasswordRoute> {
            PassKeyPasswordScreen(
                onPinComplete = { actions.onPasskeyPasswordBack() },
                onBack = actions::onPasskeyPasswordBack,
            )
        }

        composable<SettingRoute.AppLockSetupRoute> {
            AppLockSetupScreen(
                step = PinSetupStep.ENTER_NEW,
                onPinComplete = { actions.onAppLockBack() },
                onBack = actions::onAppLockBack,
            )
        }

        composable<SettingRoute.NoticeRoute> {
            NoticeListScreen(
                notices = emptyList(),
                onBackClick = actions::onNoticeBack,
            )
        }

        composable<SettingRoute.InquiryListRoute> {
            InquiryListScreen(
                inquiries = emptyList(),
                onBackClick = actions::onInquiryBack,
                onInquiryClick = actions::onNavigateToInquiryDetail,
                onNewInquiryClick = actions::onNavigateToInquiryWrite,
            )
        }

        composable<SettingRoute.InquiryDetailRoute> {
            // TODO(Afternote-BE#246): 조회 계약이 생기면
            // it.toRoute<SettingRoute.InquiryDetailRoute>().inquiryId 로 해당 문의를 조회한다.
            // 현재는 실제 데이터 소스가 없어 "찾을 수 없음" 상태를 그린다.
            InquiryDetailScreen(inquiry = null, onBackClick = actions::onInquiryBack)
        }

        composable<SettingRoute.InquiryWriteRoute> {
            InquiryWriteScreen(
                onBackClick = actions::onInquiryBack,
            )
        }
    }
}
