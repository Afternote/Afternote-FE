package com.afternote.feature.setting.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.asString
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.setting.presentation.component.PinSetupStep
import com.afternote.feature.setting.presentation.screen.AppLockSetupScreen
import com.afternote.feature.setting.presentation.screen.ConnectedAccountsScreen
import com.afternote.feature.setting.presentation.screen.DeliveryConditionScreen
import com.afternote.feature.setting.presentation.screen.NoticeListScreen
import com.afternote.feature.setting.presentation.screen.NotificationSettingScreen
import com.afternote.feature.setting.presentation.screen.PassKeyListScreen
import com.afternote.feature.setting.presentation.screen.PassKeyMakingScreen
import com.afternote.feature.setting.presentation.screen.PassKeyPasswordScreen
import com.afternote.feature.setting.presentation.screen.ProfileEditScreen
import com.afternote.feature.setting.presentation.screen.PushNotificationScreen
import com.afternote.feature.setting.presentation.screen.ReceiverEditScreen
import com.afternote.feature.setting.presentation.screen.ReceiverListScreen
import com.afternote.feature.setting.presentation.screen.ReceiverManageScreen
import com.afternote.feature.setting.presentation.screen.ReceiverRegisterScreen
import com.afternote.feature.setting.presentation.screen.SettingScreen
import com.afternote.feature.setting.presentation.screen.WithdrawConfirmScreen
import com.afternote.feature.setting.presentation.screen.WithdrawGuideScreen
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionViewModel
import com.afternote.feature.setting.presentation.viewmodel.PassKeyListIntent
import com.afternote.feature.setting.presentation.viewmodel.PassKeyListViewModel
import com.afternote.feature.setting.presentation.viewmodel.ReceiverEditViewModel
import com.afternote.feature.setting.presentation.viewmodel.ReceiverListViewModel
import com.afternote.feature.setting.presentation.viewmodel.SettingViewModel

/**
 * Settings owns its saved local stack. The parent entry owns the withdrawal ViewModel,
 * so guide/confirmation share it until this entire host is removed (#1702 boundary).
 * Home keeps its existing entry-scoped ViewModel. Direct recipient registration starts
 * at registration, so completing it returns to the caller without inserting Settings home.
 */
@Composable
public fun SettingNavHost(
    boundary: FeatureStackBoundary,
    externalActions: SettingExternalActions,
    modifier: Modifier = Modifier,
    startWithRecipientRegistration: Boolean = false,
) {
    val initialRoute =
        if (startWithRecipientRegistration) {
            SettingRoute.RecipientRegisterRoute
        } else {
            SettingRoute.SettingHomeRoute
        }
    val backStack = rememberNavBackStack(initialRoute)
    val actions =
        remember(backStack, boundary, externalActions) {
            SettingLocalNavActions(backStack, boundary, externalActions)
        }
    val hostOwner = checkNotNull(LocalViewModelStoreOwner.current)
    FeatureNavDisplay(
        backStack = backStack,
        boundary = boundary,
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<SettingRoute.SettingHomeRoute> {
                    SettingScreen(
                        onBackClick = actions::popBack,
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
                        onInquiryClick = {},
                        onNoticeClick = actions::onNavigateToNotice,
                        onTermsClick = {},
                        onPrivacyClick = {},
                        onServiceInfoClick = {},
                        onWithdrawGuideClick = actions::onNavigateToWithdrawGuide,
                    )
                }

                entry<SettingRoute.WithdrawGuideRoute> {
                    val hostViewModel: SettingViewModel = hiltViewModel(hostOwner)
                    val uiState by hostViewModel.uiState.collectAsStateWithLifecycle()
                    WithdrawGuideScreen(
                        uiState = uiState,
                        onBackClick = actions::popBack,
                        onCancelClick = actions::popBack,
                        onConfirmClick = actions::onNavigateToWithdrawConfirm,
                    )
                }

                entry<SettingRoute.WithdrawConfirmRoute> {
                    val hostViewModel: SettingViewModel = hiltViewModel(hostOwner)
                    val uiState by hostViewModel.uiState.collectAsStateWithLifecycle()
                    WithdrawConfirmScreen(
                        uiState = uiState,
                        onBackClick = actions::popBack,
                        onWithdrawSuccess = actions::onWithdrawSuccess,
                        viewModel = hostViewModel,
                    )
                }

                entry<SettingRoute.ProfileEditRoute> {
                    ProfileEditScreen(
                        onBackClick = actions::popBack,
                        onWithdrawGuideClick = actions::onNavigateToWithdrawGuide,
                    )
                }

                entry<SettingRoute.LinkedAccountRoute> {
                    ConnectedAccountsScreen(
                        onBack = actions::popBack,
                    )
                }

                entry<SettingRoute.NotificationRoute> {
                    NotificationSettingScreen(
                        onBack = actions::popBack,
                        onPushNotificationClick = actions::onNavigateToPushNotification,
                    )
                }

                entry<SettingRoute.PushNotificationRoute> {
                    PushNotificationScreen(
                        onBack = actions::popBack,
                    )
                }

                entry<SettingRoute.RecipientListRoute> { route ->
                    val viewModel: ReceiverListViewModel = hiltViewModel()
                    val receivers by viewModel.receivers.collectAsStateWithLifecycle()
                    if (route.selectForDeliveryConditions) {
                        ReceiverListScreen(
                            receivers = receivers,
                            onBackClick = actions::popBack,
                            onConfirmClick = { receiver ->
                                actions.onNavigateToAfterDelivery(receiver.receiverId)
                            },
                        )
                    } else {
                        ReceiverManageScreen(
                            receivers = receivers,
                            onBackClick = actions::popBack,
                            onReceiverClick = actions::onNavigateToRecipientEdit,
                        )
                    }
                }

                entry<SettingRoute.RecipientRegisterRoute> {
                    ReceiverRegisterScreen(
                        onBackClick = actions::popBack,
                        onRegisterSuccess = actions::popBack,
                    )
                }

                entry<SettingRoute.RecipientEditRoute> { route ->
                    ReceiverEditScreen(
                        viewModel =
                            hiltViewModel<ReceiverEditViewModel, ReceiverEditViewModel.Factory>(
                                creationCallback = { it.create(route) },
                            ),
                        onBackClick = actions::popBack,
                        onEditSuccess = actions::popBack,
                    )
                }

                entry<SettingRoute.AfterDeliveryRoute> { route ->
                    DeliveryConditionScreen(
                        viewModel =
                            hiltViewModel<DeliveryConditionViewModel, DeliveryConditionViewModel.Factory>(
                                creationCallback = { it.create(route) },
                            ),
                        onBack = actions::popBack,
                        onSaveSuccess = actions::popBack,
                        onLastGreetingEditClick = {
                            actions.onNavigateToRecipientEdit(route.receiverId)
                        },
                    )
                }

                entry<SettingRoute.PasskeyRoute> {
                    val viewModel: PassKeyListViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(PassKeyListIntent.Refresh) }
                    PassKeyListScreen(
                        passkeys = uiState.passkeys,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage?.asString(),
                        onBackClick = actions::popBack,
                        onRegisterClick = actions::onNavigateToPasskeyMaking,
                        onRetryClick = { viewModel.onIntent(PassKeyListIntent.Refresh) },
                    )
                }

                entry<SettingRoute.PasskeyMakingRoute> {
                    PassKeyMakingScreen(
                        onBackClick = actions::popBack,
                        onPasswordAuthClick = actions::onNavigateToPasskeyPassword,
                    )
                }

                entry<SettingRoute.PasskeyPasswordRoute> {
                    PassKeyPasswordScreen(
                        onPinComplete = { actions.popBack() },
                        onBack = actions::popBack,
                    )
                }

                entry<SettingRoute.AppLockSetupRoute> {
                    AppLockSetupScreen(
                        step = PinSetupStep.ENTER_NEW,
                        onPinComplete = { actions.popBack() },
                        onBack = actions::popBack,
                    )
                }

                entry<SettingRoute.NoticeRoute> {
                    NoticeListScreen(
                        notices = emptyList(),
                        onBackClick = actions::popBack,
                    )
                }
            },
    )
}
