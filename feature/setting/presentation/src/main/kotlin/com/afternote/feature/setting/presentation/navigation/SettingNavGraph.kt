package com.afternote.feature.setting.presentation.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
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
import com.afternote.core.ui.navigation.NavDestinationSurface
import com.afternote.feature.setting.presentation.account.ConnectedAccountsScreen
import com.afternote.feature.setting.presentation.applock.AppLockSetupScreen
import com.afternote.feature.setting.presentation.applock.PinSetupStep
import com.afternote.feature.setting.presentation.delivery.DeliveryConditionScreen
import com.afternote.feature.setting.presentation.home.SettingScreen
import com.afternote.feature.setting.presentation.home.SettingViewModel
import com.afternote.feature.setting.presentation.home.WithdrawConfirmScreen
import com.afternote.feature.setting.presentation.home.WithdrawGuideScreen
import com.afternote.feature.setting.presentation.notice.NoticeListScreen
import com.afternote.feature.setting.presentation.notification.NotificationSettingScreen
import com.afternote.feature.setting.presentation.notification.PushNotificationScreen
import com.afternote.feature.setting.presentation.passkey.PassKeyListScreen
import com.afternote.feature.setting.presentation.passkey.PassKeyMakingScreen
import com.afternote.feature.setting.presentation.passkey.PassKeyPasswordScreen
import com.afternote.feature.setting.presentation.passkey.PassKeyScreen
import com.afternote.feature.setting.presentation.passkey.PassKeyViewModel
import com.afternote.feature.setting.presentation.profile.ProfileEditScreen
import com.afternote.feature.setting.presentation.receiver.ReceiverEditScreen
import com.afternote.feature.setting.presentation.receiver.ReceiverListScreen
import com.afternote.feature.setting.presentation.receiver.ReceiverListViewModel
import com.afternote.feature.setting.presentation.receiver.ReceiverManageScreen
import com.afternote.feature.setting.presentation.receiver.ReceiverRegisterScreen

fun NavGraphBuilder.settingNavGraph(
    graphScopedParentEntry: () -> NavBackStackEntry,
    actions: SettingNavActions,
) {
    navigation<Route.Setting>(startDestination = SettingRoute.SettingHomeRoute) {
        settingDestination<SettingRoute.SettingHomeRoute> {
            SettingScreen(
                onBackClick = actions::onSettingBack,
                onLogoutSuccess = actions::onLogoutSuccess,
                onProfileEditClick = actions::onProfileEditClick,
                onLinkedAccountClick = actions::onLinkedAccountClick,
                onNotificationClick = actions::onNotificationClick,
                onRecipientListClick = actions::onRecipientListClick,
                onRecipientRegisterClick = actions::onRecipientRegisterClick,
                onDeliveryConditionsClick = actions::onDeliveryConditionsClick,
                onPasskeyClick = actions::onPasskeyClick,
                onAppLockClick = actions::onAppLockClick,
                onNoticeClick = actions::onNoticeClick,
                onWithdrawGuideClick = actions::onWithdrawGuideClick,
            )
        }

        settingDestination<SettingRoute.WithdrawGuideRoute> {
            val parentEntry = remember(it) { graphScopedParentEntry() }
            val viewModel: SettingViewModel = hiltViewModel(parentEntry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            WithdrawGuideScreen(
                uiState = uiState,
                onBackClick = actions::onWithdrawGuideBack,
                onCancelClick = actions::onWithdrawGuideBack,
                onConfirmClick = actions::onWithdrawConfirmClick,
            )
        }

        settingDestination<SettingRoute.WithdrawConfirmRoute> {
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

        settingDestination<SettingRoute.ProfileEditRoute> {
            ProfileEditScreen(
                onBackClick = actions::onProfileEditBack,
                onWithdrawGuideClick = actions::onWithdrawGuideClick,
            )
        }

        settingDestination<SettingRoute.LinkedAccountRoute> {
            ConnectedAccountsScreen(
                onBack = actions::onLinkedAccountBack,
            )
        }

        settingDestination<SettingRoute.NotificationRoute> {
            NotificationSettingScreen(
                onBack = actions::onNotificationBack,
                onPushNotificationClick = actions::onPushNotificationClick,
            )
        }

        settingDestination<SettingRoute.PushNotificationRoute> {
            PushNotificationScreen(
                onBack = actions::onPushNotificationBack,
            )
        }

        settingDestination<SettingRoute.RecipientListRoute> {
            val route = it.toRoute<SettingRoute.RecipientListRoute>()
            val viewModel: ReceiverListViewModel = hiltViewModel()
            val receivers by viewModel.receivers.collectAsStateWithLifecycle()
            if (route.selectForDeliveryConditions) {
                ReceiverListScreen(
                    receivers = receivers,
                    onBackClick = actions::onRecipientListBack,
                    onConfirmClick = { receiver ->
                        actions.onDeliveryConditionsRecipientSelected(receiver.receiverId)
                    },
                )
            } else {
                ReceiverManageScreen(
                    receivers = receivers,
                    onBackClick = actions::onRecipientListBack,
                    onReceiverClick = actions::onRecipientEditClick,
                    onRegisterClick = actions::onRecipientRegisterClick,
                )
            }
        }

        settingDestination<SettingRoute.RecipientRegisterRoute> {
            ReceiverRegisterScreen(
                onBackClick = actions::onRecipientRegisterBack,
                onRegisterSuccess = actions::onRecipientRegisterBack,
            )
        }

        settingDestination<SettingRoute.RecipientEditRoute> {
            ReceiverEditScreen(
                onBackClick = actions::onRecipientEditBack,
                onEditSuccess = actions::onRecipientEditBack,
            )
        }

        settingDestination<SettingRoute.AfterDeliveryRoute> {
            val route = it.toRoute<SettingRoute.AfterDeliveryRoute>()
            DeliveryConditionScreen(
                onBack = actions::onAfterDeliveryBack,
                onSaveSuccess = actions::onAfterDeliveryBack,
                onLastGreetingEditClick = {
                    actions.onRecipientEditClick(route.receiverId)
                },
            )
        }

        settingDestination<SettingRoute.PasskeyRoute> {
            val viewModel: PassKeyViewModel = hiltViewModel()
            val isPasskeyRegistered by viewModel.isPasskeyRegistered.collectAsStateWithLifecycle()
            if (isPasskeyRegistered == true) {
                PassKeyListScreen(onBackClick = actions::onPasskeyBack)
            } else if (isPasskeyRegistered == false) {
                PassKeyScreen(
                    onBackClick = actions::onPasskeyBack,
                    onRegisterClick = actions::onPasskeyRegisterClick,
                )
            }
        }

        settingDestination<SettingRoute.PasskeyMakingRoute> {
            PassKeyMakingScreen(
                onBackClick = actions::onPasskeyMakingBack,
                onPasswordAuthClick = actions::onPasswordAuthClick,
            )
        }

        settingDestination<SettingRoute.PasskeyPasswordRoute> {
            PassKeyPasswordScreen(
                onPinComplete = { actions.onPasskeyPasswordBack() },
                onBack = actions::onPasskeyPasswordBack,
            )
        }

        settingDestination<SettingRoute.AppLockSetupRoute> {
            AppLockSetupScreen(
                step = PinSetupStep.ENTER_NEW,
                onPinComplete = { actions.onAppLockBack() },
                onBack = actions::onAppLockBack,
            )
        }

        settingDestination<SettingRoute.NoticeRoute> {
            NoticeListScreen(
                notices = emptyList(),
                onBackClick = actions::onNoticeBack,
            )
        }
    }
}

/**
 * 설정 목적지 등록. `composable<T>` 와 같되 화면을 [NavDestinationSurface] 로 감싼다 (#2145).
 *
 * 설정 화면 파일 16개가 `Scaffold(containerColor = Color.Transparent)` 라, 감싸지 않으면 predictive back
 * 진행 중 줄어든 앞 화면 사이로 뒤 화면 글자가 비친다. 화면마다 `containerColor` 를 고치지 않고 등록
 * 자리에서 한 번 칠한다.
 *
 * 설정이 Navigation 3 로컬 스택으로 옮겨 가면(#1695 사슬) 이 파일째 사라지고 core/ui 의 표준
 * entry 데코레이터가 같은 일을 받는다.
 */
private inline fun <reified T : Any> NavGraphBuilder.settingDestination(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T> { entry -> NavDestinationSurface { content(entry) } }
}
