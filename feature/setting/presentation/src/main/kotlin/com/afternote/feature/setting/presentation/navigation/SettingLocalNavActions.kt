package com.afternote.feature.setting.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.core.ui.navigation.popOrExit

internal class SettingLocalNavActions(
    private val backStack: NavBackStack<NavKey>,
    private val boundary: FeatureStackBoundary,
    private val externalActions: SettingExternalActions,
) : SettingNavActions {
    override fun popBack(): Unit = backStack.popOrExit(boundary)

    override fun onLogoutSuccess(): Unit = externalActions.onLogoutSuccess()

    override fun onWithdrawSuccess(): Unit = externalActions.onWithdrawSuccess()

    override fun onNavigateToWithdrawGuide() {
        backStack.add(SettingRoute.WithdrawGuideRoute)
    }

    override fun onNavigateToWithdrawConfirm() {
        backStack.add(SettingRoute.WithdrawConfirmRoute)
    }

    override fun onNavigateToProfileEdit() {
        backStack.add(SettingRoute.ProfileEditRoute)
    }

    override fun onNavigateToLinkedAccount() {
        backStack.add(SettingRoute.LinkedAccountRoute)
    }

    override fun onNavigateToNotification() {
        backStack.add(SettingRoute.NotificationRoute)
    }

    override fun onNavigateToPushNotification() {
        backStack.add(SettingRoute.PushNotificationRoute)
    }

    override fun onNavigateToRecipientList() {
        backStack.add(SettingRoute.RecipientListRoute())
    }

    override fun onNavigateToRecipientListForDeliveryConditions() {
        backStack.add(
            SettingRoute.RecipientListRoute(selectForDeliveryConditions = true),
        )
    }

    override fun onNavigateToRecipientRegister() {
        backStack.add(SettingRoute.RecipientRegisterRoute)
    }

    override fun onNavigateToRecipientEdit(receiverId: Long) {
        backStack.add(SettingRoute.RecipientEditRoute(receiverId))
    }

    override fun onNavigateToAfterDelivery(receiverId: Long) {
        backStack.add(SettingRoute.AfterDeliveryRoute(receiverId))
    }

    override fun onNavigateToPasskey() {
        backStack.add(SettingRoute.PasskeyRoute)
    }

    override fun onNavigateToPasskeyMaking() {
        backStack.add(SettingRoute.PasskeyMakingRoute)
    }

    override fun onNavigateToPasskeyPassword() {
        backStack.add(SettingRoute.PasskeyPasswordRoute)
    }

    override fun onNavigateToAppLock() {
        backStack.add(SettingRoute.AppLockSetupRoute)
    }

    override fun onNavigateToNotice() {
        backStack.add(SettingRoute.NoticeRoute)
    }
}
