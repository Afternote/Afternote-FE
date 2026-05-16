package com.afternote.feature.setting.presentation.viewmodel

sealed interface ProfileEditUiState {
    data object Loading : ProfileEditUiState

    data class Success(
        val name: String,
        val phone: String,
        val email: String,
        val isUpdating: Boolean = false,
    ) : ProfileEditUiState

    data object Error : ProfileEditUiState
}

sealed interface ProfileEditEvent {
    data object UpdateSuccess : ProfileEditEvent
    data object UpdateFailure : ProfileEditEvent
}
