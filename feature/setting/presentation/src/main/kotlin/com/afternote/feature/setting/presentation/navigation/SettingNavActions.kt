package com.afternote.feature.setting.presentation.navigation

interface SettingNavActions {
    fun onSettingBack()

    fun onLogoutSuccess()

    // 앱 계층이 실제 수신자 그래프 이동을 붙이기 전까지의 단계적 기본값. 통합 시 추상으로 조인다.
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
