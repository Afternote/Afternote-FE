package com.afternote.feature.setting.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
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
                runCatching { userRepository.getMyPushSettings() }
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
            _uiState.update { it.copy(isNewsletterOn = on) }
            viewModelScope.launch {
                runCatching { userRepository.updateMyPushSettings(timeLetter = on, mindRecord = null, afterNote = null) }
                    .onSuccess { Log.d(TAG, "onNewsletterToggle: success, on=$on") }
                    .onFailure { e ->
                        Log.e(TAG, "onNewsletterToggle: failed, on=$on", e)
                        _uiState.update { it.copy(isNewsletterOn = !on) }
                    }
            }
        }

        fun onMindRecordToggle(on: Boolean) {
            _uiState.update { it.copy(isMindRecordOn = on) }
            viewModelScope.launch {
                runCatching { userRepository.updateMyPushSettings(timeLetter = null, mindRecord = on, afterNote = null) }
                    .onSuccess { Log.d(TAG, "onMindRecordToggle: success, on=$on") }
                    .onFailure { e ->
                        Log.e(TAG, "onMindRecordToggle: failed, on=$on", e)
                        _uiState.update { it.copy(isMindRecordOn = !on) }
                    }
            }
        }

        fun onAfternoteToggle(on: Boolean) {
            _uiState.update { it.copy(isAfternoteOn = on) }
            viewModelScope.launch {
                runCatching { userRepository.updateMyPushSettings(timeLetter = null, mindRecord = null, afterNote = on) }
                    .onSuccess { Log.d(TAG, "onAfternoteToggle: success, on=$on") }
                    .onFailure { e ->
                        Log.e(TAG, "onAfternoteToggle: failed, on=$on", e)
                        _uiState.update { it.copy(isAfternoteOn = !on) }
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
