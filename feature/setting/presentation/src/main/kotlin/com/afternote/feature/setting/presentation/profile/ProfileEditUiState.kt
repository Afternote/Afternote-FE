package com.afternote.feature.setting.presentation.profile

import com.afternote.core.ui.mvi.UiState

internal sealed interface ProfileEditUiState : UiState {
    data object Loading : ProfileEditUiState

    data class Success(
        val name: String,
        val phone: String,
        val email: String,
        val isUpdating: Boolean = false,
        /** 수정을 마쳤다는 사실. 소비되는 [pendingEvent] 와 달리 되돌리지 않아, 화면이 닫히는 동안에도 재제출을 막는다. */
        val isUpdated: Boolean = false,
        val pendingEvent: ProfileEditEvent? = null,
    ) : ProfileEditUiState {
        val isUpdateLocked: Boolean get() = isUpdating || isUpdated
    }

    data object Error : ProfileEditUiState
}

internal sealed interface ProfileEditEvent {
    data object UpdateSuccess : ProfileEditEvent

    data object UpdateFailure : ProfileEditEvent
}
