package com.afternote.feature.setting.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Job
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

        /** 진행 중인 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null
        private var pendingUpdates = 0

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — VM 필드인 이유는 ReceiverHomeViewModel 의 refreshOnReturn 과
         * 동일, 프로세스 사망 후 복원에서도 init 로드와 수명이 일치한다.
         */
        private var isFirstResume = true

        init {
            refresh()
        }

        /** 다른 화면에서 복귀했을 때의 자동 갱신 (#701). 첫 진입은 건너뛰고, 로드가 겹치면 건너뛴다. */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            if (loadJob?.isActive == true || pendingUpdates > 0) return
            refresh(isAutomatic = true)
        }

        private fun refresh(isAutomatic: Boolean = false) {
            val deviceAlarmOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            Log.d(TAG, "refresh: deviceAlarmOn=$deviceAlarmOn")
            _uiState.update { it.copy(isDeviceAlarmOn = deviceAlarmOn) }
            loadPushSettings(isAutomatic)
        }

        private fun loadPushSettings(isAutomatic: Boolean) {
            loadJob =
                viewModelScope.launch {
                    Log.d(TAG, "loadPushSettings: start")
                    if (!isAutomatic) _uiState.update { it.copy(isLoading = true) }
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

        fun onSmsChecked(checked: Boolean) = _uiState.update { it.copy(isSmsChecked = checked) }

        fun onEmailChecked(checked: Boolean) = _uiState.update { it.copy(isEmailChecked = checked) }

        fun onPushChecked(checked: Boolean) = _uiState.update { it.copy(isPushChecked = checked) }

        fun onNewsletterToggle(on: Boolean) {
            loadJob?.cancel()
            _uiState.update { it.copy(isNewsletterOn = on) }
            updatePushSettings {
                runCatchingCancellable { userRepository.updateMyPushSettings(timeLetter = on, mindRecord = null, afterNote = null) }
                    .onSuccess { Log.d(TAG, "onNewsletterToggle: success, on=$on") }
                    .onFailure { e ->
                        Log.e(TAG, "onNewsletterToggle: failed, on=$on", e)
                        _uiState.update { it.copy(isNewsletterOn = !on) }
                    }
            }
        }

        fun onMindRecordToggle(on: Boolean) {
            loadJob?.cancel()
            _uiState.update { it.copy(isMindRecordOn = on) }
            updatePushSettings {
                runCatchingCancellable { userRepository.updateMyPushSettings(timeLetter = null, mindRecord = on, afterNote = null) }
                    .onSuccess { Log.d(TAG, "onMindRecordToggle: success, on=$on") }
                    .onFailure { e ->
                        Log.e(TAG, "onMindRecordToggle: failed, on=$on", e)
                        _uiState.update { it.copy(isMindRecordOn = !on) }
                    }
            }
        }

        fun onAfternoteToggle(on: Boolean) {
            loadJob?.cancel()
            _uiState.update { it.copy(isAfternoteOn = on) }
            updatePushSettings {
                runCatchingCancellable { userRepository.updateMyPushSettings(timeLetter = null, mindRecord = null, afterNote = on) }
                    .onSuccess { Log.d(TAG, "onAfternoteToggle: success, on=$on") }
                    .onFailure { e ->
                        Log.e(TAG, "onAfternoteToggle: failed, on=$on", e)
                        _uiState.update { it.copy(isAfternoteOn = !on) }
                    }
            }
        }

        private fun updatePushSettings(update: suspend () -> Unit) {
            pendingUpdates++
            viewModelScope.launch {
                try {
                    update()
                } finally {
                    pendingUpdates--
                }
            }
        }

        private companion object {
            private const val TAG = "PushNotificationVM"
        }
    }
