package com.afternote.feature.setting.presentation.notification

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.domain.result.runCatchingCancellable
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.SettingNotificationRepository
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
internal class PushNotificationViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val notificationRepository: SettingNotificationRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<PushNotificationIntent, PushNotificationUiState, PushNotificationReducerEvent>(
            PushNotificationUiState(isLoading = true),
        ) {
        private var loadJob: Job? = null

        init {
            refreshDeviceAlarmStatus()
            loadPushSettings()
            loadMarketingConsents()
        }

        override fun onIntent(intent: PushNotificationIntent) {
            when (intent) {
                PushNotificationIntent.RetryLoad -> {
                    if (currentState.errorMessage != null) loadPushSettings()
                }

                PushNotificationIntent.RefreshDeviceAlarmStatus -> {
                    refreshDeviceAlarmStatus()
                }

                is PushNotificationIntent.ChangeMarketingConsent -> {
                    updateMarketingConsent(intent.consent, intent.checked)
                }

                is PushNotificationIntent.TogglePushSetting -> {
                    updatePushSetting(PushSettingUpdate(intent.setting, intent.on))
                }

                PushNotificationIntent.RetrySave -> {
                    retrySave()
                }

                PushNotificationIntent.DismissSaveFailure -> {
                    dispatch(PushNotificationReducerEvent.SaveFailureDismissed)
                }

                PushNotificationIntent.MarketingFeedbackStarted -> {
                    dispatch(PushNotificationReducerEvent.MarketingFeedbackActiveChanged(active = true))
                }

                PushNotificationIntent.MarketingFeedbackStopped -> {
                    dispatch(PushNotificationReducerEvent.MarketingFeedbackActiveChanged(active = false))
                }

                PushNotificationIntent.ConsumeMarketingConsentSaveFailure -> {
                    dispatch(PushNotificationReducerEvent.MarketingConsentSaveFailureConsumed)
                }
            }
        }

        override fun reduce(
            state: PushNotificationUiState,
            event: PushNotificationReducerEvent,
        ): PushNotificationUiState =
            when (event) {
                is PushNotificationReducerEvent.DeviceAlarmStatusRead -> {
                    state.copy(isDeviceAlarmOn = event.on)
                }

                PushNotificationReducerEvent.PushSettingsLoading -> {
                    state.copy(isLoading = true, errorMessage = null)
                }

                is PushNotificationReducerEvent.PushSettingsLoaded -> {
                    state.copy(
                        isLoading = false,
                        errorMessage = null,
                        isNewsletterOn = event.setting.timeLetter,
                        isMindRecordOn = event.setting.mindRecord,
                        isAfternoteOn = event.setting.afterNote,
                    )
                }

                PushNotificationReducerEvent.PushSettingsLoadFailed -> {
                    state.copy(isLoading = false, errorMessage = UiText.Resource(R.string.setting_push_load_error))
                }

                is PushNotificationReducerEvent.MarketingConsentsLoaded -> {
                    state.copy(
                        isSmsChecked = event.consent.sms,
                        isEmailChecked = event.consent.email,
                        isPushChecked = event.consent.push,
                    )
                }

                is PushNotificationReducerEvent.MarketingConsentChanged -> {
                    state.withMarketingConsent(event.consent, event.checked)
                }

                // 화면이 STARTED 밖이면 값만 되돌리고 안내 신호는 올리지 않는다 (#558).
                is PushNotificationReducerEvent.MarketingConsentSaveFailed -> {
                    state
                        .withMarketingConsent(event.consent, !event.requested)
                        .copy(isMarketingConsentSaveFailed = state.isMarketingConsentSaveFailed || state.isMarketingFeedbackActive)
                }

                is PushNotificationReducerEvent.PushSettingSaving -> {
                    state.withValue(event.update.setting, event.update.on).withUpdating(event.update.setting, updating = true)
                }

                is PushNotificationReducerEvent.PushSettingSaved -> {
                    state.withUpdating(event.setting, updating = false)
                }

                is PushNotificationReducerEvent.PushSettingSaveFailed -> {
                    state
                        .withValue(event.update.setting, event.previous)
                        .withUpdating(event.update.setting, updating = false)
                        .copy(saveFailure = event.failure, failedUpdate = event.update)
                }

                PushNotificationReducerEvent.SaveFailureDismissed -> {
                    state.copy(saveFailure = null, failedUpdate = null)
                }

                // 비활성으로 바뀌면 아직 소비되지 않은 안내도 걷는다 — 다음 진입에 재생하지 않는다.
                is PushNotificationReducerEvent.MarketingFeedbackActiveChanged -> {
                    state.copy(
                        isMarketingFeedbackActive = event.active,
                        isMarketingConsentSaveFailed = state.isMarketingConsentSaveFailed && event.active,
                    )
                }

                PushNotificationReducerEvent.MarketingConsentSaveFailureConsumed -> {
                    state.copy(isMarketingConsentSaveFailed = false)
                }
            }

        private fun refreshDeviceAlarmStatus() {
            val deviceAlarmOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            Log.d(TAG, "refreshDeviceAlarmStatus: deviceAlarmOn=$deviceAlarmOn")
            dispatch(PushNotificationReducerEvent.DeviceAlarmStatusRead(deviceAlarmOn))
        }

        private fun loadPushSettings() {
            if (loadJob?.isActive == true) return
            dispatch(PushNotificationReducerEvent.PushSettingsLoading)
            loadJob =
                viewModelScope.launch {
                    Log.d(TAG, "loadPushSettings: start")
                    runCatchingCancellable { notificationRepository.getMyPushSettings() }
                        .onSuccess { setting ->
                            Log.d(TAG, "loadPushSettings: success=$setting")
                            dispatch(PushNotificationReducerEvent.PushSettingsLoaded(setting))
                        }.onFailure { e ->
                            // 조회 실패는 Logcat 에만 남긴다. 반복 조회 잡음이 저장 실패 진단의 보관 한도를 밀어내지 않게 한다 (#963).
                            Log.e(TAG, "loadPushSettings: failed", e)
                            dispatch(PushNotificationReducerEvent.PushSettingsLoadFailed)
                        }
                }
        }

        private fun loadMarketingConsents() {
            viewModelScope.launch {
                Log.d(TAG, "loadMarketingConsents: start")
                runCatchingCancellable { notificationRepository.getMyMarketingConsents() }
                    .onSuccess { consent ->
                        Log.d(TAG, "loadMarketingConsents: success=$consent")
                        dispatch(PushNotificationReducerEvent.MarketingConsentsLoaded(consent))
                    }.onFailure { e ->
                        Log.e(TAG, "loadMarketingConsents: failed", e)
                    }
            }
        }

        private fun updateMarketingConsent(
            consent: MarketingConsent,
            checked: Boolean,
        ) {
            dispatch(PushNotificationReducerEvent.MarketingConsentChanged(consent, checked))
            viewModelScope.launch {
                runCatchingCancellable {
                    notificationRepository.updateMyMarketingConsents(
                        sms = checked.takeIf { consent == MarketingConsent.SMS },
                        email = checked.takeIf { consent == MarketingConsent.EMAIL },
                        push = checked.takeIf { consent == MarketingConsent.PUSH },
                    )
                }.onSuccess {
                    Log.d(TAG, "updateMarketingConsent: success, consent=$consent, checked=$checked")
                }.onFailure { e ->
                    errorReporter.recordFailure(e, mapOf(KEY_STAGE to consent.reportingStage()))
                    dispatch(PushNotificationReducerEvent.MarketingConsentSaveFailed(consent, requested = checked))
                }
            }
        }

        private fun retrySave() {
            val update = currentState.failedUpdate ?: return
            dispatch(PushNotificationReducerEvent.SaveFailureDismissed)
            updatePushSetting(update)
        }

        private fun updatePushSetting(update: PushSettingUpdate) {
            if (currentState.isLoading || currentState.errorMessage != null || currentState.isUpdating(update.setting)) return
            val previousValue = currentState.valueOf(update.setting)
            dispatch(PushNotificationReducerEvent.PushSettingSaving(update))
            viewModelScope.launch {
                runCatchingCancellable {
                    notificationRepository.updateMyPushSettings(
                        timeLetter = update.on.takeIf { update.setting == PushSetting.NEWSLETTER },
                        mindRecord = update.on.takeIf { update.setting == PushSetting.MIND_RECORD },
                        afterNote = update.on.takeIf { update.setting == PushSetting.AFTERNOTE },
                    )
                }.onSuccess {
                    dispatch(PushNotificationReducerEvent.PushSettingSaved(update.setting))
                }.onFailure { failure ->
                    errorReporter.recordFailure(
                        failure,
                        mapOf(
                            KEY_STAGE to STAGE_PUSH_SETTING_UPDATE,
                            KEY_PUSH_SETTING to update.setting.reportingName(),
                        ),
                    )
                    dispatch(PushNotificationReducerEvent.PushSettingSaveFailed(update, previousValue, failure.toSaveFailure()))
                }
            }
        }

        companion object {
            private const val TAG = "PushNotificationVM"
            private const val KEY_STAGE = "stage"
            private const val KEY_PUSH_SETTING = "push_setting"
            private const val STAGE_PUSH_SETTING_UPDATE = "push_setting_update"
        }
    }

/** 진단 속성 값. 토글 종류만 담는 고정 문자열이라 사용자 정보가 섞이지 않는다. */
private fun PushSetting.reportingName(): String =
    when (this) {
        PushSetting.NEWSLETTER -> "newsletter"
        PushSetting.MIND_RECORD -> "mind_record"
        PushSetting.AFTERNOTE -> "afternote"
    }

/** 마케팅 동의 저장 실패의 진단 단계. 채널 이름만 담는 고정 문자열이다. */
private fun MarketingConsent.reportingStage(): String =
    when (this) {
        MarketingConsent.SMS -> "sms_consent_update"
        MarketingConsent.EMAIL -> "email_consent_update"
        MarketingConsent.PUSH -> "push_consent_update"
    }

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

private fun PushNotificationUiState.withMarketingConsent(
    consent: MarketingConsent,
    checked: Boolean,
): PushNotificationUiState =
    when (consent) {
        MarketingConsent.SMS -> copy(isSmsChecked = checked)
        MarketingConsent.EMAIL -> copy(isEmailChecked = checked)
        MarketingConsent.PUSH -> copy(isPushChecked = checked)
    }

private fun Throwable.toSaveFailure(): PushNotificationSaveFailure =
    when (this) {
        is PushSettingFailure.NetworkUnavailable -> PushNotificationSaveFailure.NETWORK
        else -> PushNotificationSaveFailure.SERVER
    }
