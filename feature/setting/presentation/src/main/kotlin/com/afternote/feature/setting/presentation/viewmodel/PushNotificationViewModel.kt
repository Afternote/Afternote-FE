package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class PushNotificationViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(PushNotificationUiState())
        val uiState: StateFlow<PushNotificationUiState> = _uiState.asStateFlow()

        fun onSmsChecked(checked: Boolean) = _uiState.update { it.copy(isSmsChecked = checked) }

        fun onEmailChecked(checked: Boolean) = _uiState.update { it.copy(isEmailChecked = checked) }

        fun onPushChecked(checked: Boolean) = _uiState.update { it.copy(isPushChecked = checked) }

        fun onNewsletterToggle(on: Boolean) = _uiState.update { it.copy(isNewsletterOn = on) }

        fun onMindRecordToggle(on: Boolean) = _uiState.update { it.copy(isMindRecordOn = on) }

        fun onAfternoteToggle(on: Boolean) = _uiState.update { it.copy(isAfternoteOn = on) }
    }
