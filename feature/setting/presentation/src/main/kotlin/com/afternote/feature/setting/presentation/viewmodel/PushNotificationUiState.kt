package com.afternote.feature.setting.presentation.viewmodel

data class PushNotificationUiState(
    val isLoading: Boolean = false,
    val isDeviceAlarmOn: Boolean = false,
    // 마케팅 알림 (기기 알림 꺼졌을 때)
    val isSmsChecked: Boolean = true,
    val isEmailChecked: Boolean = true,
    val isPushChecked: Boolean = false,
    // 푸시 알림 토글 (기기 알림 켜졌을 때)
    val isNewsletterOn: Boolean = false,
    val isMindRecordOn: Boolean = false,
    val isAfternoteOn: Boolean = false,
)

sealed interface PushNotificationEvent {
    data object MarketingConsentSaveFailed : PushNotificationEvent
}
