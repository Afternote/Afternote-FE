package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface PushNotificationIntent : MviIntent {
    data object RefreshDeviceAlarmStatus : PushNotificationIntent

    data class SmsChecked(
        val checked: Boolean,
    ) : PushNotificationIntent

    data class EmailChecked(
        val checked: Boolean,
    ) : PushNotificationIntent

    data class PushChecked(
        val checked: Boolean,
    ) : PushNotificationIntent

    data class NewsletterToggle(
        val on: Boolean,
    ) : PushNotificationIntent

    data class MindRecordToggle(
        val on: Boolean,
    ) : PushNotificationIntent

    data class AfternoteToggle(
        val on: Boolean,
    ) : PushNotificationIntent

    data object SaveFailureDismiss : PushNotificationIntent

    data object SaveFailureRetry : PushNotificationIntent

    data class MarketingFeedbackActive(
        val active: Boolean,
    ) : PushNotificationIntent

    data object ConsumeMarketingFailure : PushNotificationIntent
}

internal sealed interface PushNotificationReducerEvent : ReducerEvent {
    data class DeviceAlarmChanged(
        val on: Boolean,
    ) : PushNotificationReducerEvent

    data object Loading : PushNotificationReducerEvent

    data class Loaded(
        val setting: UserPushSetting,
    ) : PushNotificationReducerEvent

    data object LoadFailed : PushNotificationReducerEvent

    data class MarketingLoaded(
        val consent: UserMarketingConsent,
    ) : PushNotificationReducerEvent

    data class MarketingChanged(
        val setting: MarketingSetting,
        val checked: Boolean,
    ) : PushNotificationReducerEvent

    data class MarketingFailed(
        val setting: MarketingSetting,
        val previous: Boolean,
    ) : PushNotificationReducerEvent

    data class Saving(
        val update: PushSettingUpdate,
    ) : PushNotificationReducerEvent

    data class Saved(
        val setting: PushSetting,
    ) : PushNotificationReducerEvent

    data class SaveFailed(
        val update: PushSettingUpdate,
        val previous: Boolean,
        val failure: PushNotificationSaveFailure,
    ) : PushNotificationReducerEvent

    data object FailureDismissed : PushNotificationReducerEvent

    data class MarketingFeedbackChanged(
        val active: Boolean,
    ) : PushNotificationReducerEvent

    data object MarketingFailureConsumed : PushNotificationReducerEvent
}

internal enum class MarketingSetting { SMS, EMAIL, PUSH }
