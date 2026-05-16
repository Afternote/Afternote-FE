package com.afternote.feature.setting.presentation.viewmodel

import android.content.Context
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
            _uiState.update {
                it.copy(isDeviceAlarmOn = NotificationManagerCompat.from(context).areNotificationsEnabled())
            }
            loadPushSettings()
        }

        private fun loadPushSettings() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                runCatching { userRepository.getMyPushSettings() }
                    .onSuccess { setting ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isNewsletterOn = setting.timeLetter,
                                isMindRecordOn = setting.mindRecord,
                                isAfternoteOn = setting.afterNote,
                            )
                        }
                    }
                    .onFailure { _uiState.update { it.copy(isLoading = false) } }
            }
        }

        fun onSmsChecked(checked: Boolean) = _uiState.update { it.copy(isSmsChecked = checked) }

        fun onEmailChecked(checked: Boolean) = _uiState.update { it.copy(isEmailChecked = checked) }

        fun onPushChecked(checked: Boolean) = _uiState.update { it.copy(isPushChecked = checked) }

        fun onNewsletterToggle(on: Boolean) {
            _uiState.update { it.copy(isNewsletterOn = on) }
            viewModelScope.launch {
                runCatching { userRepository.updateMyPushSettings(timeLetter = on, mindRecord = null, afterNote = null) }
                    .onFailure { _uiState.update { it.copy(isNewsletterOn = !on) } }
            }
        }

        fun onMindRecordToggle(on: Boolean) {
            _uiState.update { it.copy(isMindRecordOn = on) }
            viewModelScope.launch {
                runCatching { userRepository.updateMyPushSettings(timeLetter = null, mindRecord = on, afterNote = null) }
                    .onFailure { _uiState.update { it.copy(isMindRecordOn = !on) } }
            }
        }

        fun onAfternoteToggle(on: Boolean) {
            _uiState.update { it.copy(isAfternoteOn = on) }
            viewModelScope.launch {
                runCatching { userRepository.updateMyPushSettings(timeLetter = null, mindRecord = null, afterNote = on) }
                    .onFailure { _uiState.update { it.copy(isAfternoteOn = !on) } }
            }
        }
    }
