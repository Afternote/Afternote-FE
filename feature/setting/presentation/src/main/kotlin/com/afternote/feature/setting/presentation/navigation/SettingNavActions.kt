package com.afternote.feature.setting.presentation.navigation

interface SettingNavActions {
    fun onSettingBack()

    fun onLogoutSuccess()

    fun onWithdrawGuideClick()

    fun onWithdrawConfirmClick()

    fun onWithdrawGuideBack()

    fun onWithdrawConfirmBack()

    fun onWithdrawSuccess()

    fun onProfileEditClick()

    fun onProfileEditBack()

    fun onPasswordChangeClick()

    fun onPasswordChangeBack()

    fun onLinkedAccountClick()

    fun onLinkedAccountBack()

    fun onNotificationClick()

    fun onNotificationBack()

    fun onPushNotificationClick()

    fun onPushNotificationBack()

    fun onRecipientListClick()

    fun onDeliveryConditionsClick()

    fun onRecipientListBack()

    fun onRecipientRegisterClick()

    fun onRecipientRegisterBack()

    fun onRecipientEditClick(receiverId: Long)

    fun onRecipientEditBack()

    fun onDeliveryConditionsRecipientSelected(receiverId: Long)

    fun onAfterDeliveryBack()

    fun onPasskeyClick()

    fun onPasskeyBack()

    fun onPasskeyRegisterClick()

    fun onPasskeyMakingBack()

    fun onPasswordAuthClick()

    fun onPasskeyPasswordBack()

    fun onAppLockClick()

    fun onAppLockBack()

    fun onNoticeClick()

    fun onNoticeBack()
}
