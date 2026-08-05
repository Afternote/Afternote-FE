package com.afternote.feature.setting.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PushNotificationViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PushNotificationUiState())
        val uiState: StateFlow<PushNotificationUiState> = _uiState.asStateFlow()

        init {
            val deviceAlarmOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            Log.d(TAG, "init: deviceAlarmOn=$deviceAlarmOn")
            _uiState.update { it.copy(isDeviceAlarmOn = deviceAlarmOn) }
            loadPushSettings()
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

        fun onSmsChecked(checked: Boolean) = _uiState.update { it.copy(isSmsChecked = checked) }

        fun onEmailChecked(checked: Boolean) = _uiState.update { it.copy(isEmailChecked = checked) }

        fun onPushChecked(checked: Boolean) = _uiState.update { it.copy(isPushChecked = checked) }

        fun onNewsletterToggle(on: Boolean) {
            _uiState.update { it.copy(isNewsletterOn = on) }
            viewModelScope.launch {
                runCatching { userRepository.updateMyPushSettings(timeLetter = on, mindRecord = null, afterNote = null) }
                    .onSuccess { Log.d(TAG, "onNewsletterToggle: success, on=$on") }
                    .onFailure { e ->
                        Log.e(TAG, "onNewsletterToggle: failed, on=$on", e)
                        _uiState.update { it.copy(isNewsletterOn = !on, showSaveFailure = true) }
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
                        _uiState.update { it.copy(isMindRecordOn = !on, showSaveFailure = true) }
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
                        _uiState.update { it.copy(isAfternoteOn = !on, showSaveFailure = true) }
                    }
            }
        }

        fun onSaveFailureShown() {
            _uiState.update { it.copy(showSaveFailure = false) }
        }

        companion object {
            private const val TAG = "PushNotificationVM"
        }
    }
