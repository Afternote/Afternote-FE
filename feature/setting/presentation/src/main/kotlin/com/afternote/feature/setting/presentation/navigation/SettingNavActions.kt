package com.afternote.feature.setting.presentation.navigation

internal interface SettingNavActions {
    fun popBack()

    fun onLogoutSuccess()

    fun onWithdrawGuideClick()

    fun onWithdrawConfirmClick()

    fun onWithdrawSuccess()

    fun onProfileEditClick()

    fun onLinkedAccountClick()

    fun onNotificationClick()

    fun onPushNotificationClick()

    fun onRecipientListClick()

    fun onDeliveryConditionsClick()

    fun onRecipientRegisterClick()

    fun onRecipientEditClick(receiverId: Long)

    fun onDeliveryConditionsRecipientSelected(receiverId: Long)

    fun onPasskeyClick()

    fun onPasskeyRegisterClick()

    fun onPasswordAuthClick()

    fun onAppLockClick()

    fun onNoticeClick()
}
