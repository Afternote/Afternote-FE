package com.afternote.feature.setting.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PushNotificationViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val userRepository: UserRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PushNotificationUiState())
        val uiState: StateFlow<PushNotificationUiState> = _uiState.asStateFlow()

        // 화면이 없는 동안의 안내는 다음 진입에 재생하지 않는다.
        private val _events =
            MutableSharedFlow<PushNotificationEvent>(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val events = _events.asSharedFlow()

        // 슬롯 하나 — 서로 다른 토글이 연달아 실패해도 재시도 대상은 마지막 실패만 남긴다.
        // 이미 실패한 토글은 위에서 이전 값으로 롤백되어 화면에 "안 켜짐"으로 보이므로,
        // 조용히 사라지는 것은 재시도 "대상"뿐이다. 여러 실패를 동시에 재시도하는 요구가
        // 없어 의도적으로 단순화했다 (#558 리뷰 합의).
        private var failedUpdate: PushSettingUpdate? = null

        init {
            refreshDeviceAlarmStatus()
            loadPushSettings()
            loadMarketingConsents()
        }

        fun refreshDeviceAlarmStatus() {
            val deviceAlarmOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            Log.d(TAG, "refreshDeviceAlarmStatus: deviceAlarmOn=$deviceAlarmOn")
            _uiState.update { it.copy(isDeviceAlarmOn = deviceAlarmOn) }
        }

        private fun loadPushSettings() {
            viewModelScope.launch {
                Log.d(TAG, "loadPushSettings: start")
                _uiState.update { it.copy(isLoading = true) }
                runCatchingCancellable { userRepository.getMyPushSettings() }
                    .onSuccess { setting ->
                        Log.d(TAG, "loadPushSettings: success=$setting")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isNewsletterOn = setting.timeLetter,
                                isMindRecordOn = setting.mindRecord,
                                isAfternoteOn = setting.afterNote,
                            )
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "loadPushSettings: failed", e)
                        _uiState.update { it.copy(isLoading = false) }
                    }
            }
        }

        private fun loadMarketingConsents() {
            viewModelScope.launch {
                Log.d(TAG, "loadMarketingConsents: start")
                runCatching { userRepository.getMyMarketingConsents() }
                    .onSuccess { consent ->
                        Log.d(TAG, "loadMarketingConsents: success=$consent")
                        _uiState.update {
                            it.copy(
                                isSmsChecked = consent.sms,
                                isEmailChecked = consent.email,
                                isPushChecked = consent.push,
                            )
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "loadMarketingConsents: failed", e)
                    }
            }
        }

        fun onSmsChecked(checked: Boolean) {
            _uiState.update { it.copy(isSmsChecked = checked) }
            viewModelScope.launch {
                runCatchingCancellable { userRepository.updateMyMarketingConsents(sms = checked, email = null, push = null) }
                    .onSuccess { Log.d(TAG, "onSmsChecked: success, checked=$checked") }
                    .onFailure { e ->
                        errorReporter.recordFailure(e, mapOf(KEY_STAGE to STAGE_SMS_CONSENT))
                        _uiState.update { it.copy(isSmsChecked = !checked) }
                        _events.tryEmit(PushNotificationEvent.MarketingConsentSaveFailed)
                    }
            }
        }

        fun onEmailChecked(checked: Boolean) {
            _uiState.update { it.copy(isEmailChecked = checked) }
            viewModelScope.launch {
                runCatchingCancellable { userRepository.updateMyMarketingConsents(sms = null, email = checked, push = null) }
                    .onSuccess { Log.d(TAG, "onEmailChecked: success, checked=$checked") }
                    .onFailure { e ->
                        errorReporter.recordFailure(e, mapOf(KEY_STAGE to STAGE_EMAIL_CONSENT))
                        _uiState.update { it.copy(isEmailChecked = !checked) }
                        _events.tryEmit(PushNotificationEvent.MarketingConsentSaveFailed)
                    }
            }
        }

        fun onPushChecked(checked: Boolean) {
            _uiState.update { it.copy(isPushChecked = checked) }
            viewModelScope.launch {
                runCatchingCancellable { userRepository.updateMyMarketingConsents(sms = null, email = null, push = checked) }
                    .onSuccess { Log.d(TAG, "onPushChecked: success, checked=$checked") }
                    .onFailure { e ->
                        errorReporter.recordFailure(e, mapOf(KEY_STAGE to STAGE_PUSH_CONSENT))
                        _uiState.update { it.copy(isPushChecked = !checked) }
                        _events.tryEmit(PushNotificationEvent.MarketingConsentSaveFailed)
                    }
            }
        }

        fun onNewsletterToggle(on: Boolean) {
            updatePushSetting(PushSettingUpdate(PushSetting.NEWSLETTER, on))
        }

        fun onMindRecordToggle(on: Boolean) {
            updatePushSetting(PushSettingUpdate(PushSetting.MIND_RECORD, on))
        }

        fun onAfternoteToggle(on: Boolean) {
            updatePushSetting(PushSettingUpdate(PushSetting.AFTERNOTE, on))
        }

        fun onSaveFailureDismiss() {
            failedUpdate = null
            _uiState.update { it.copy(saveFailure = null) }
        }

        fun onSaveFailureRetry() {
            val update = failedUpdate ?: return
            failedUpdate = null
            _uiState.update { it.copy(saveFailure = null) }
            updatePushSetting(update)
        }

        private fun updatePushSetting(update: PushSettingUpdate) {
            if (_uiState.value.isUpdating(update.setting)) return
            val previousValue = _uiState.value.valueOf(update.setting)
            _uiState.update {
                it.withValue(update.setting, update.on).withUpdating(update.setting, updating = true)
            }
            viewModelScope.launch {
                runCatchingCancellable {
                    userRepository.updateMyPushSettings(
                        timeLetter = update.on.takeIf { update.setting == PushSetting.NEWSLETTER },
                        mindRecord = update.on.takeIf { update.setting == PushSetting.MIND_RECORD },
                        afterNote = update.on.takeIf { update.setting == PushSetting.AFTERNOTE },
                    )
                }.onSuccess {
                    _uiState.update { it.withUpdating(update.setting, updating = false) }
                }.onFailure { failure ->
                    failedUpdate = update
                    _uiState.update {
                        it
                            .withValue(update.setting, previousValue)
                            .withUpdating(update.setting, updating = false)
                            .copy(saveFailure = failure.toSaveFailure())
                    }
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

private enum class PushSetting {
    NEWSLETTER,
    MIND_RECORD,
    AFTERNOTE,
}

private data class PushSettingUpdate(
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
