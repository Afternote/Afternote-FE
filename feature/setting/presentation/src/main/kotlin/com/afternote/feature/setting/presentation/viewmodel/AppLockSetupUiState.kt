package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.UiState

internal data class AppLockSetupUiState(
    val pin: String = "",
    val isComplete: Boolean = false,
) : UiState
