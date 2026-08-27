package com.afternote.feature.setting.presentation.navigation

interface SettingNavActions {
    fun popBack()

    fun onLogoutSuccess()

    fun onNavigateToWithdrawGuide()

    fun onNavigateToWithdrawConfirm()

    fun onWithdrawSuccess()

    fun onNavigateToProfileEdit()

    fun onNavigateToLinkedAccount()

    fun onNavigateToNotification()

    fun onNavigateToRecipientList()

    fun onNavigateToRecipientListForDeliveryConditions()

    fun onNavigateToRecipientRegister()

    fun onNavigateToRecipientEdit(receiverId: Long)

    fun onNavigateToAfterDelivery(receiverId: Long)

    fun onNavigateToPasskey()

    fun onNavigateToPasskeyMaking()

    fun onNavigateToPasskeyPassword()

    fun onNavigateToAppLock()

    fun onNavigateToNotice()
}
