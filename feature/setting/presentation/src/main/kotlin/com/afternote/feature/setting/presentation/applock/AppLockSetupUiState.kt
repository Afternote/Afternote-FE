package com.afternote.feature.setting.presentation.applock

import com.afternote.core.ui.mvi.UiState

internal data class AppLockSetupUiState(
    val pin: String = "",
    val isComplete: Boolean = false,
) : UiState
