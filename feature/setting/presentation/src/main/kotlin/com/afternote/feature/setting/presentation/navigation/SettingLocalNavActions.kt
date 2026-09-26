package com.afternote.feature.setting.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureNavigationCallbacks
import com.afternote.core.ui.navigation.popOrExit

/**
 * 설정 화면 콜백을 로컬 백스택 조작으로 잇는다.
 *
 * 컴포저블이 아니라 평범한 클래스다 — 백스택 «모양» 을 컴포지션 없이 재려는 것이다(#1601).
 */
internal class SettingLocalNavActions(
    private val backStack: NavBackStack<NavKey>,
    private val navigationCallbacks: FeatureNavigationCallbacks,
    private val externalActions: SettingExternalActions,
) : SettingNavActions {
    override fun popBack(): Unit = backStack.popOrExit(navigationCallbacks)

    override fun onLogoutSuccess(): Unit = externalActions.onLogoutSuccess()

    override fun onWithdrawSuccess(): Unit = externalActions.onWithdrawSuccess()

    override fun onWithdrawGuideClick() {
        backStack.add(SettingRoute.WithdrawGuideRoute)
    }

    override fun onWithdrawConfirmClick() {
        backStack.add(SettingRoute.WithdrawConfirmRoute)
    }

    override fun onProfileEditClick() {
        backStack.add(SettingRoute.ProfileEditRoute)
    }

    override fun onPasswordChangeClick() {
        backStack.add(SettingRoute.PasswordChangeRoute)
    }

    override fun onLinkedAccountClick() {
        backStack.add(SettingRoute.LinkedAccountRoute)
    }

    override fun onNotificationClick() {
        backStack.add(SettingRoute.NotificationRoute)
    }

    override fun onPushNotificationClick() {
        backStack.add(SettingRoute.PushNotificationRoute)
    }

    override fun onRecipientListClick() {
        backStack.add(SettingRoute.RecipientListRoute())
    }

    override fun onDeliveryConditionsClick() {
        backStack.add(SettingRoute.RecipientListRoute(selectForDeliveryConditions = true))
    }

    override fun onRecipientRegisterClick() {
        backStack.add(SettingRoute.RecipientRegisterRoute)
    }

    override fun onRecipientEditClick(receiverId: Long) {
        backStack.add(SettingRoute.RecipientEditRoute(receiverId))
    }

    override fun onDeliveryConditionsRecipientSelected(receiverId: Long) {
        backStack.add(SettingRoute.AfterDeliveryRoute(receiverId))
    }

    override fun onPasskeyClick() {
        backStack.add(SettingRoute.PasskeyRoute)
    }

    override fun onPasskeyRegisterClick() {
        backStack.add(SettingRoute.PasskeyMakingRoute)
    }

    override fun onPasswordAuthClick() {
        backStack.add(SettingRoute.PasskeyPasswordRoute)
    }

    override fun onAppLockClick() {
        backStack.add(SettingRoute.AppLockSetupRoute)
    }

    override fun onNoticeClick() {
        backStack.add(SettingRoute.NoticeRoute)
    }
}
