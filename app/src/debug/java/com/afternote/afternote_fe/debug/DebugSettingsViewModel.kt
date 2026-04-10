package com.afternote.afternote_fe.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.data.network.MockModePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DebugSettingsViewModel
    @Inject
    constructor(
        private val mockModePreferences: MockModePreferences,
    ) : ViewModel() {
        val isMockEnabled: StateFlow<Boolean> =
            mockModePreferences.isEnabledFlow
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = mockModePreferences.isEnabled,
                )

        fun toggleMockMode() {
            mockModePreferences.isEnabled = !mockModePreferences.isEnabled
        }
    }
