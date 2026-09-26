package com.afternote.feature.setting.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureNavigationCallbacks
import com.afternote.feature.setting.presentation.account.ConnectedAccountsScreen
import com.afternote.feature.setting.presentation.applock.AppLockSetupScreen
import com.afternote.feature.setting.presentation.applock.PinSetupStep
import com.afternote.feature.setting.presentation.delivery.DeliveryConditionScreen
import com.afternote.feature.setting.presentation.delivery.DeliveryConditionViewModel
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
import com.afternote.feature.setting.presentation.password.PasswordChangeScreen
import com.afternote.feature.setting.presentation.profile.ProfileEditScreen
import com.afternote.feature.setting.presentation.receiver.ReceiverEditScreen
import com.afternote.feature.setting.presentation.receiver.ReceiverEditViewModel
import com.afternote.feature.setting.presentation.receiver.ReceiverListRouteContent
import com.afternote.feature.setting.presentation.receiver.ReceiverListViewModel
import com.afternote.feature.setting.presentation.receiver.ReceiverRegisterScreen

/**
 * 설정 피처가 소유하는 로컬 Navigation 3 스택 (#1695).
 *
 * ## 루트와의 계약 (#1702 가 소비한다)
 *
 * 루트가 주는 것은 둘이다. [navigationCallbacks] 는 스택 바닥에서의 back 을 받아 이 host 를 루트
 * 백스택에서 내리고, [externalActions] 는 인증 상태가 바뀐 뒤(로그아웃·탈퇴) 루트 스택을 갈아
 * 끼운다. 설정 안에서는 바텀바가 보이지 않으므로 깊이 신호는 쓰지 않는다.
 *
 * host 가 갖는 것은 스택과 그 시작점, 그리고 탈퇴 두 화면이 공유하는 [SettingViewModel] 의 수명이다.
 * 루트가 `NavDisplay` 로 바뀌어도 이 경계는 그대로고 콜백의 구현만 갈린다.
 *
 * ## 시작점
 *
 * 홈의 «수신인 지정 미완료» 칩은 설정 홈을 거치지 않고 등록 화면으로 곧장 들어온다
 * ([startWithRecipientRegistration]). 등록 화면을 스택 바닥에 두면 등록 성공의 pop 이 곧 host 이탈이라,
 * 이관 전처럼 부른 곳(홈)으로 돌아간다. 설정 홈·목록에서 들어온 등록은 그 위에 쌓이므로 거기로 돌아간다.
 *
 * ## 탈퇴 안내·확인이 공유하는 ViewModel
 *
 * Nav2 에서는 `Route.Setting` 그래프 엔트리에 묶여 있었다. Nav3 엔 그래프 계층이 없으므로 이
 * 컴포저블을 담은 상위 entry 의 스토어(`hostOwner`)에 올린다 — host 가 루트 백스택에서 내려갈 때
 * 정리되므로 이관 전과 같은 수명이다. host 몸통에서 미리 만들지 않는 것은 `init` 이 프로필을
 * 불러오기 때문이다. 탈퇴 화면에 들어갈 때 처음 만들어지는 것도 이관 전과 같다. 설정 홈은 제 entry
 * 범위의 인스턴스를 따로 쓴다.
 *
 * ## route 인자
 *
 * Nav3 entry 는 `SavedStateHandle` 에 route 인자를 채워 주지 않는다. 인자를 받는 ViewModel 은 키를
 * assisted 로 받고, entry 가 `creationCallback` 으로 키를 넘긴다.
 */
@Composable
public fun SettingNavHost(
    navigationCallbacks: FeatureNavigationCallbacks,
    externalActions: SettingExternalActions,
    modifier: Modifier = Modifier,
    startWithRecipientRegistration: Boolean = false,
) {
    val backStack =
        rememberNavBackStack(
            if (startWithRecipientRegistration) SettingRoute.RecipientRegisterRoute else SettingRoute.SettingHomeRoute,
        )
    val actions =
        remember(backStack, navigationCallbacks, externalActions) {
            SettingLocalNavActions(backStack, navigationCallbacks, externalActions)
        }
    val hostOwner = checkNotNull(LocalViewModelStoreOwner.current) { "설정 host 를 담은 entry 의 ViewModelStoreOwner 가 없다" }

    FeatureNavDisplay(
        backStack = backStack,
        boundary = navigationCallbacks,
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<SettingRoute.SettingHomeRoute> {
                    SettingScreen(
                        onBackClick = actions::popBack,
                        onLogoutSuccess = actions::onLogoutSuccess,
                        onProfileEditClick = actions::onProfileEditClick,
                        onPasswordChangeClick = actions::onPasswordChangeClick,
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

                entry<SettingRoute.WithdrawGuideRoute> {
                    val viewModel: SettingViewModel = hiltViewModel(hostOwner)
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    WithdrawGuideScreen(
                        uiState = uiState,
                        onBackClick = actions::popBack,
                        onCancelClick = actions::popBack,
                        onConfirmClick = actions::onWithdrawConfirmClick,
                    )
                }

                entry<SettingRoute.WithdrawConfirmRoute> {
                    val viewModel: SettingViewModel = hiltViewModel(hostOwner)
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    WithdrawConfirmScreen(
                        uiState = uiState,
                        onBackClick = actions::popBack,
                        onWithdrawSuccess = actions::onWithdrawSuccess,
                        viewModel = viewModel,
                    )
                }

                entry<SettingRoute.ProfileEditRoute> {
                    ProfileEditScreen(
                        onBackClick = actions::popBack,
                        onWithdrawGuideClick = actions::onWithdrawGuideClick,
                    )
                }

                entry<SettingRoute.PasswordChangeRoute> {
                    PasswordChangeScreen(
                        onBackClick = actions::popBack,
                        onChanged = actions::popBack,
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
                        onPushNotificationClick = actions::onPushNotificationClick,
                    )
                }

                entry<SettingRoute.PushNotificationRoute> {
                    PushNotificationScreen(
                        onBack = actions::popBack,
                    )
                }

                entry<SettingRoute.RecipientListRoute> { key ->
                    val viewModel: ReceiverListViewModel = hiltViewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    ReceiverListRouteContent(
                        uiState = uiState,
                        selectForDeliveryConditions = key.selectForDeliveryConditions,
                        onBackClick = actions::popBack,
                        onRetryClick = viewModel::retry,
                        onConfirmClick = { receiver ->
                            actions.onDeliveryConditionsRecipientSelected(receiver.receiverId)
                        },
                        onReceiverClick = actions::onRecipientEditClick,
                        onRegisterClick = actions::onRecipientRegisterClick,
                    )
                }

                entry<SettingRoute.RecipientRegisterRoute> {
                    ReceiverRegisterScreen(
                        onBackClick = actions::popBack,
                        onRegisterSuccess = actions::popBack,
                    )
                }

                entry<SettingRoute.RecipientEditRoute> { key ->
                    ReceiverEditScreen(
                        onBackClick = actions::popBack,
                        onEditSuccess = actions::popBack,
                        viewModel =
                            hiltViewModel<ReceiverEditViewModel, ReceiverEditViewModel.Factory>(
                                creationCallback = { factory -> factory.create(key) },
                            ),
                    )
                }

                entry<SettingRoute.AfterDeliveryRoute> { key ->
                    DeliveryConditionScreen(
                        onBack = actions::popBack,
                        onSaveSuccess = actions::popBack,
                        onLastGreetingEditClick = { actions.onRecipientEditClick(key.receiverId) },
                        viewModel =
                            hiltViewModel<DeliveryConditionViewModel, DeliveryConditionViewModel.Factory>(
                                creationCallback = { factory -> factory.create(key) },
                            ),
                    )
                }

                entry<SettingRoute.PasskeyRoute> {
                    val viewModel: PassKeyViewModel = hiltViewModel()
                    val isPasskeyRegistered by viewModel.isPasskeyRegistered.collectAsStateWithLifecycle()
                    if (isPasskeyRegistered == true) {
                        PassKeyListScreen(onBackClick = actions::popBack)
                    } else if (isPasskeyRegistered == false) {
                        PassKeyScreen(
                            onBackClick = actions::popBack,
                            onRegisterClick = actions::onPasskeyRegisterClick,
                        )
                    }
                }

                entry<SettingRoute.PasskeyMakingRoute> {
                    PassKeyMakingScreen(
                        onBackClick = actions::popBack,
                        onPasswordAuthClick = actions::onPasswordAuthClick,
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
