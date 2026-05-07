package com.afternote.feature.setting.presentation.viewmodel

data class AppLockSetupUiState(
    val pin: String = "",
    val isComplete: Boolean = false,
)
