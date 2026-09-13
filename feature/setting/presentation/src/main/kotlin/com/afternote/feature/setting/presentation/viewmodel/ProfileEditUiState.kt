package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.UiState

internal sealed interface ProfileEditUiState : UiState {
    data object Loading : ProfileEditUiState

    data class Success(
        val name: String,
        val phone: String,
        val email: String,
        val isUpdating: Boolean = false,
        val pendingEvent: ProfileEditEvent? = null,
    ) : ProfileEditUiState

    data object Error : ProfileEditUiState
}

internal sealed interface ProfileEditEvent {
    data object UpdateSuccess : ProfileEditEvent

    data object UpdateFailure : ProfileEditEvent
}
