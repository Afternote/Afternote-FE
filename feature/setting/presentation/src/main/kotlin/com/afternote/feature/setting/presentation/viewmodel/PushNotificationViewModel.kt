package com.afternote.feature.setting.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
internal class PushNotificationViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val userRepository: UserRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<PushNotificationIntent, PushNotificationUiState, PushNotificationReducerEvent>(PushNotificationUiState()) {
        override fun onIntent(intent: PushNotificationIntent) {
            when (intent) {
                PushNotificationIntent.RefreshDeviceAlarmStatus -> {
                    refreshDeviceAlarmStatus()
                }

                is PushNotificationIntent.SmsChecked -> {
                    onSmsChecked(intent.checked)
                }

                is PushNotificationIntent.EmailChecked -> {
                    onEmailChecked(intent.checked)
                }

                is PushNotificationIntent.PushChecked -> {
                    onPushChecked(intent.checked)
                }

                is PushNotificationIntent.NewsletterToggle -> {
                    onNewsletterToggle(intent.on)
                }

                is PushNotificationIntent.MindRecordToggle -> {
                    onMindRecordToggle(intent.on)
                }

                is PushNotificationIntent.AfternoteToggle -> {
                    onAfternoteToggle(intent.on)
                }

                PushNotificationIntent.SaveFailureDismiss -> {
                    onSaveFailureDismiss()
                }

                PushNotificationIntent.SaveFailureRetry -> {
                    onSaveFailureRetry()
                }

                is PushNotificationIntent.MarketingFeedbackActive -> {
                    dispatch(
                        PushNotificationReducerEvent.MarketingFeedbackChanged(intent.active),
                    )
                }

                PushNotificationIntent.ConsumeMarketingFailure -> {
                    dispatch(PushNotificationReducerEvent.MarketingFailureConsumed)
                }
            }
        }

        override fun reduce(
            state: PushNotificationUiState,
            event: PushNotificationReducerEvent,
        ): PushNotificationUiState =
            when (event) {
                is PushNotificationReducerEvent.DeviceAlarmChanged -> {
                    state.copy(isDeviceAlarmOn = event.on)
                }

                PushNotificationReducerEvent.Loading -> {
                    state.copy(isLoading = true)
                }

                is PushNotificationReducerEvent.Loaded -> {
                    state.copy(
                        isLoading = false,
                        isNewsletterOn = event.setting.timeLetter,
                        isMindRecordOn = event.setting.mindRecord,
                        isAfternoteOn = event.setting.afterNote,
                    )
                }

                PushNotificationReducerEvent.LoadFailed -> {
                    state.copy(isLoading = false)
                }

                is PushNotificationReducerEvent.MarketingLoaded -> {
                    state.copy(isSmsChecked = event.consent.sms, isEmailChecked = event.consent.email, isPushChecked = event.consent.push)
                }

                is PushNotificationReducerEvent.MarketingChanged -> {
                    state.withMarketingValue(event.setting, event.checked)
                }

                is PushNotificationReducerEvent.MarketingFailed -> {
                    state.withMarketingValue(event.setting, event.previous).copy(
                        // #558의 기존 경계: 알림 설정 화면이 없는 동안의 실패 안내는 재진입에 재생하지 않는다.
                        pendingEvent = if (state.isMarketingFeedbackActive) PushNotificationEvent.MarketingConsentSaveFailed else null,
                    )
                }

                is PushNotificationReducerEvent.Saving -> {
                    state.withValue(event.update.setting, event.update.on).withUpdating(event.update.setting, true)
                }

                is PushNotificationReducerEvent.Saved -> {
                    state.withUpdating(event.setting, false)
                }

                is PushNotificationReducerEvent.SaveFailed -> {
                    state
                        .withValue(
                            event.update.setting,
                            event.previous,
                        ).withUpdating(event.update.setting, false)
                        .copy(saveFailure = event.failure, failedUpdate = event.update)
                }

                PushNotificationReducerEvent.FailureDismissed -> {
                    state.copy(saveFailure = null, failedUpdate = null)
                }

                is PushNotificationReducerEvent.MarketingFeedbackChanged -> {
                    state.copy(isMarketingFeedbackActive = event.active, pendingEvent = state.pendingEvent.takeIf { event.active })
                }

                PushNotificationReducerEvent.MarketingFailureConsumed -> {
                    state.copy(pendingEvent = null)
                }
            }

        init {
            refreshDeviceAlarmStatus()
            loadPushSettings()
            loadMarketingConsents()
        }

        private fun refreshDeviceAlarmStatus() {
            val deviceAlarmOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            Log.d(TAG, "refreshDeviceAlarmStatus: deviceAlarmOn=$deviceAlarmOn")
            dispatch(PushNotificationReducerEvent.DeviceAlarmChanged(deviceAlarmOn))
        }

        private fun loadPushSettings() {
            viewModelScope.launch {
                Log.d(TAG, "loadPushSettings: start")
                dispatch(PushNotificationReducerEvent.Loading)
                runCatchingCancellable { userRepository.getMyPushSettings() }
                    .onSuccess { setting ->
                        Log.d(TAG, "loadPushSettings: success=$setting")
                        dispatch(PushNotificationReducerEvent.Loaded(setting))
                    }.onFailure { e ->
                        Log.e(TAG, "loadPushSettings: failed", e)
                        dispatch(PushNotificationReducerEvent.LoadFailed)
                    }
            }
        }

        private fun loadMarketingConsents() {
            viewModelScope.launch {
                Log.d(TAG, "loadMarketingConsents: start")
                runCatching { userRepository.getMyMarketingConsents() }
                    .onSuccess { consent ->
                        Log.d(TAG, "loadMarketingConsents: success=$consent")
                        dispatch(PushNotificationReducerEvent.MarketingLoaded(consent))
                    }.onFailure { e ->
                        Log.e(TAG, "loadMarketingConsents: failed", e)
                    }
            }
        }

        private fun onSmsChecked(checked: Boolean) {
            dispatch(PushNotificationReducerEvent.MarketingChanged(MarketingSetting.SMS, checked))
            viewModelScope.launch {
                runCatchingCancellable { userRepository.updateMyMarketingConsents(sms = checked, email = null, push = null) }
                    .onSuccess { Log.d(TAG, "onSmsChecked: success, checked=$checked") }
                    .onFailure { e ->
                        errorReporter.recordFailure(e, mapOf(KEY_STAGE to STAGE_SMS_CONSENT))
                        dispatch(PushNotificationReducerEvent.MarketingFailed(MarketingSetting.SMS, !checked))
                    }
            }
        }

        private fun onEmailChecked(checked: Boolean) {
            dispatch(PushNotificationReducerEvent.MarketingChanged(MarketingSetting.EMAIL, checked))
            viewModelScope.launch {
                runCatchingCancellable { userRepository.updateMyMarketingConsents(sms = null, email = checked, push = null) }
                    .onSuccess { Log.d(TAG, "onEmailChecked: success, checked=$checked") }
                    .onFailure { e ->
                        errorReporter.recordFailure(e, mapOf(KEY_STAGE to STAGE_EMAIL_CONSENT))
                        dispatch(PushNotificationReducerEvent.MarketingFailed(MarketingSetting.EMAIL, !checked))
                    }
            }
        }

        private fun onPushChecked(checked: Boolean) {
            dispatch(PushNotificationReducerEvent.MarketingChanged(MarketingSetting.PUSH, checked))
            viewModelScope.launch {
                runCatchingCancellable { userRepository.updateMyMarketingConsents(sms = null, email = null, push = checked) }
                    .onSuccess { Log.d(TAG, "onPushChecked: success, checked=$checked") }
                    .onFailure { e ->
                        errorReporter.recordFailure(e, mapOf(KEY_STAGE to STAGE_PUSH_CONSENT))
                        dispatch(PushNotificationReducerEvent.MarketingFailed(MarketingSetting.PUSH, !checked))
                    }
            }
        }

        private fun onNewsletterToggle(on: Boolean) {
            updatePushSetting(PushSettingUpdate(PushSetting.NEWSLETTER, on))
        }

        private fun onMindRecordToggle(on: Boolean) {
            updatePushSetting(PushSettingUpdate(PushSetting.MIND_RECORD, on))
        }

        private fun onAfternoteToggle(on: Boolean) {
            updatePushSetting(PushSettingUpdate(PushSetting.AFTERNOTE, on))
        }

        private fun onSaveFailureDismiss() {
            dispatch(PushNotificationReducerEvent.FailureDismissed)
        }

        private fun onSaveFailureRetry() {
            val update = currentState.failedUpdate ?: return
            dispatch(PushNotificationReducerEvent.FailureDismissed)
            updatePushSetting(update)
        }

        private fun updatePushSetting(update: PushSettingUpdate) {
            if (currentState.isUpdating(update.setting)) return
            val previousValue = currentState.valueOf(update.setting)
            dispatch(PushNotificationReducerEvent.Saving(update))
            viewModelScope.launch {
                runCatchingCancellable {
                    userRepository.updateMyPushSettings(
                        timeLetter = update.on.takeIf { update.setting == PushSetting.NEWSLETTER },
                        mindRecord = update.on.takeIf { update.setting == PushSetting.MIND_RECORD },
                        afterNote = update.on.takeIf { update.setting == PushSetting.AFTERNOTE },
                    )
                }.onSuccess {
                    dispatch(PushNotificationReducerEvent.Saved(update.setting))
                }.onFailure { failure ->
                    dispatch(PushNotificationReducerEvent.SaveFailed(update, previousValue, failure.toSaveFailure()))
                }
            }
        }

        companion object {
            private const val TAG = "PushNotificationVM"
            private const val KEY_STAGE = "stage"
            private const val STAGE_SMS_CONSENT = "sms_consent_update"
            private const val STAGE_EMAIL_CONSENT = "email_consent_update"
            private const val STAGE_PUSH_CONSENT = "push_consent_update"
        }
    }

internal enum class PushSetting {
    NEWSLETTER,
    MIND_RECORD,
    AFTERNOTE,
}

internal data class PushSettingUpdate(
    val setting: PushSetting,
    val on: Boolean,
)

private fun PushNotificationUiState.valueOf(setting: PushSetting): Boolean =
    when (setting) {
        PushSetting.NEWSLETTER -> isNewsletterOn
        PushSetting.MIND_RECORD -> isMindRecordOn
        PushSetting.AFTERNOTE -> isAfternoteOn
    }

private fun PushNotificationUiState.withValue(
    setting: PushSetting,
    on: Boolean,
): PushNotificationUiState =
    when (setting) {
        PushSetting.NEWSLETTER -> copy(isNewsletterOn = on)
        PushSetting.MIND_RECORD -> copy(isMindRecordOn = on)
        PushSetting.AFTERNOTE -> copy(isAfternoteOn = on)
    }

private fun PushNotificationUiState.isUpdating(setting: PushSetting): Boolean =
    when (setting) {
        PushSetting.NEWSLETTER -> isNewsletterUpdating
        PushSetting.MIND_RECORD -> isMindRecordUpdating
        PushSetting.AFTERNOTE -> isAfternoteUpdating
    }

private fun PushNotificationUiState.withUpdating(
    setting: PushSetting,
    updating: Boolean,
): PushNotificationUiState =
    when (setting) {
        PushSetting.NEWSLETTER -> copy(isNewsletterUpdating = updating)
        PushSetting.MIND_RECORD -> copy(isMindRecordUpdating = updating)
        PushSetting.AFTERNOTE -> copy(isAfternoteUpdating = updating)
    }

private fun Throwable.toSaveFailure(): PushNotificationSaveFailure =
    when (this) {
        is PushSettingFailure.NetworkUnavailable -> PushNotificationSaveFailure.NETWORK
        else -> PushNotificationSaveFailure.SERVER
    }

private fun PushNotificationUiState.withMarketingValue(
    setting: MarketingSetting,
    checked: Boolean,
): PushNotificationUiState =
    when (setting) {
        MarketingSetting.SMS -> copy(isSmsChecked = checked)
        MarketingSetting.EMAIL -> copy(isEmailChecked = checked)
        MarketingSetting.PUSH -> copy(isPushChecked = checked)
    }
