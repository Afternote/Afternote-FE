package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.UiState

internal data class ReceiverRegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val pendingEvent: ReceiverRegisterEvent? = null,
) : UiState

internal sealed interface ReceiverRegisterEvent {
    data object RegisterSuccess : ReceiverRegisterEvent
}
