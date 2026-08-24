package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.model.user.ReceiverDetail

data class ReceiverEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val receiver: ReceiverDetail? = null,
    val errorMessage: String? = null,
)

sealed interface ReceiverEditEvent {
    data object EditSuccess : ReceiverEditEvent
}
