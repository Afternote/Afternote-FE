package com.afternote.feature.setting.presentation.notification

import com.afternote.core.ui.mvi.UiState

internal data class PushNotificationUiState(
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
    val isNewsletterUpdating: Boolean = false,
    val isMindRecordUpdating: Boolean = false,
    val isAfternoteUpdating: Boolean = false,
    val saveFailure: PushNotificationSaveFailure? = null,
    /**
     * [saveFailure] 에서 재시도할 변경. [saveFailure] 와 같은 전이에서 함께 채우고 함께 비운다.
     *
     * 슬롯 하나 — 서로 다른 토글이 연달아 실패해도 재시도 대상은 마지막 실패만 남긴다.
     * 이미 실패한 토글은 이전 값으로 롤백되어 화면에 "안 켜짐"으로 보이므로,
     * 조용히 사라지는 것은 재시도 "대상"뿐이다. 여러 실패를 동시에 재시도하는 요구가
     * 없어 의도적으로 단순화했다 (#558 리뷰 합의).
     */
    val failedUpdate: PushSettingUpdate? = null,
    /** 알림 설정 화면이 STARTED 인 동안만 true 다. 이때만 마케팅 동의 저장 실패를 안내 신호로 올린다 (#558). */
    val isMarketingFeedbackActive: Boolean = false,
    /** 마케팅 동의 저장 실패 안내 신호. 화면이 스낵바를 띄우고 소비 Intent 로 내린다 (#1502). */
    val isMarketingConsentSaveFailed: Boolean = false,
) : UiState

enum class PushNotificationSaveFailure {
    NETWORK,
    SERVER,
}

/** 기기 알림이 켜졌을 때 받는 서비스 알림 종류. */
internal enum class PushSetting {
    NEWSLETTER,
    MIND_RECORD,
    AFTERNOTE,
}

internal data class PushSettingUpdate(
    val setting: PushSetting,
    val on: Boolean,
)
