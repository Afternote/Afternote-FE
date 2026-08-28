package com.afternote.feature.setting.presentation.navigation

interface SettingNavActions {
    fun onSettingBack()

    fun onLogoutSuccess()

    // 앱 계층 구현이 붙기 전까지 단독 컴파일을 위한 단계적 기본값.
    fun onNavigateToReceivedRecords() {}

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

    fun onNavigateToRecipientListForDeliveryConditions()

    fun onRecipientListBack()

    fun onNavigateToRecipientRegister()

    fun onRecipientRegisterBack()

    fun onNavigateToRecipientEdit(receiverId: Long)

    fun onRecipientEditBack()

    fun onNavigateToAfterDelivery(receiverId: Long)

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
