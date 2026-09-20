package com.afternote.feature.setting.presentation.applock

data class AppLockSetupUiState(
    val pin: String = "",
    val isComplete: Boolean = false,
)
