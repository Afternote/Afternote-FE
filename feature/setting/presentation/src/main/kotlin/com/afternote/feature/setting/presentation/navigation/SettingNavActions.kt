package com.afternote.feature.setting.presentation.navigation

/**
 * 설정 화면 콜백이 부르는 이동 명령.
 *
 * 화면마다 있던 `on<Screen>Back` 은 전부 [popBack] 하나다 — 로컬 스택에서는 어느 화면이든
 * back 이 «한 칸 내리기» 라 이름을 나눌 이유가 없다. 인증 상태를 바꾸는 둘([onLogoutSuccess]·
 * [onWithdrawSuccess])은 루트 스택을 비워야 해서 [SettingExternalActions] 로 나간다.
 */
internal interface SettingNavActions {
    fun popBack()

    fun onLogoutSuccess()

    fun onWithdrawSuccess()

    fun onWithdrawGuideClick()

    fun onWithdrawConfirmClick()

    fun onProfileEditClick()

    fun onPasswordChangeClick()

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
