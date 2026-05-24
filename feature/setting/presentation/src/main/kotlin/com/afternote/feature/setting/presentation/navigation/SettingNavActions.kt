package com.afternote.feature.setting.presentation.navigation

interface SettingNavActions {
    fun onSettingBack()

    fun onLogoutSuccess()

    fun onNavigateToWithdrawGuide()

    fun onNavigateToWithdrawConfirm()

    fun onWithdrawGuideBack()

    fun onWithdrawConfirmBack()

    fun onWithdrawSuccess()

    fun onNavigateToProfileEdit()

    fun onProfileEditBack()

    fun onNavigateToLinkedAccount()

    fun onLinkedAccountBack()

    fun onNavigateToNotification()

    fun onNotificationBack()

    fun onNavigateToRecipientList()

    fun onRecipientListBack()

    fun onNavigateToRecipientRegister()

    fun onRecipientRegisterBack()

    fun onNavigateToAfterDelivery()

    fun onAfterDeliveryBack()

    fun onNavigateToPasskey()

    fun onPasskeyBack()

    fun onNavigateToPasskeyMaking()

    fun onPasskeyMakingBack()

    fun onNavigateToPasskeyPassword()

    fun onPasskeyPasswordBack()

    fun onNavigateToAppLock()

    fun onAppLockBack()

    fun onNavigateToNotice()

    fun onNoticeBack()
}
