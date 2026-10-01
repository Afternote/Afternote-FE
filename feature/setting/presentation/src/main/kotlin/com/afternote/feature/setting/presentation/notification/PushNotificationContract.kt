package com.afternote.feature.setting.presentation.notification

import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface PushNotificationIntent : MviIntent {
    data object RetryLoad : PushNotificationIntent

    /** 기기 알림 허용 여부를 다시 읽는다. 화면 재개와 권한 요청 결과 때 보낸다. */
    data object RefreshDeviceAlarmStatus : PushNotificationIntent

    data class ChangeMarketingConsent(
        val consent: MarketingConsent,
        val checked: Boolean,
    ) : PushNotificationIntent

    data class TogglePushSetting(
        val setting: PushSetting,
        val on: Boolean,
    ) : PushNotificationIntent

    data object RetrySave : PushNotificationIntent

    data object DismissSaveFailure : PushNotificationIntent

    /**
     * 알림 설정 화면이 STARTED 에 들어섰다. 이때부터 마케팅 동의 저장 실패를 안내 신호로 올린다.
     *
     * STARTED 밖에서 난 실패는 신호로 남기지 않고, [MarketingFeedbackStopped] 가 남은 신호도 걷는다.
     * 화면이 없는 동안의 안내를 다음 진입에 재생하지 않는 #558 계약이다.
     */
    data object MarketingFeedbackStarted : PushNotificationIntent

    data object MarketingFeedbackStopped : PushNotificationIntent

    data object ConsumeMarketingConsentSaveFailure : PushNotificationIntent
}

internal sealed interface PushNotificationReducerEvent : ReducerEvent {
    data class DeviceAlarmStatusRead(
        val on: Boolean,
    ) : PushNotificationReducerEvent

    data object PushSettingsLoading : PushNotificationReducerEvent

    data class PushSettingsLoaded(
        val setting: UserPushSetting,
    ) : PushNotificationReducerEvent

    data object PushSettingsLoadFailed : PushNotificationReducerEvent

    data class MarketingConsentsLoaded(
        val consent: UserMarketingConsent,
    ) : PushNotificationReducerEvent

    data class MarketingConsentChanged(
        val consent: MarketingConsent,
        val checked: Boolean,
    ) : PushNotificationReducerEvent

    /** [requested] 로 바꾸려던 저장이 실패했다. 값은 그 반대로 되돌린다. */
    data class MarketingConsentSaveFailed(
        val consent: MarketingConsent,
        val requested: Boolean,
    ) : PushNotificationReducerEvent

    data class PushSettingSaving(
        val update: PushSettingUpdate,
    ) : PushNotificationReducerEvent

    data class PushSettingSaved(
        val setting: PushSetting,
    ) : PushNotificationReducerEvent

    data class PushSettingSaveFailed(
        val update: PushSettingUpdate,
        val previous: Boolean,
        val failure: PushNotificationSaveFailure,
    ) : PushNotificationReducerEvent

    data object SaveFailureDismissed : PushNotificationReducerEvent

    data class MarketingFeedbackActiveChanged(
        val active: Boolean,
    ) : PushNotificationReducerEvent

    data object MarketingConsentSaveFailureConsumed : PushNotificationReducerEvent
}

/** 기기 알림이 꺼졌을 때 받는 마케팅 알림 채널. */
internal enum class MarketingConsent {
    SMS,
    EMAIL,
    PUSH,
}
