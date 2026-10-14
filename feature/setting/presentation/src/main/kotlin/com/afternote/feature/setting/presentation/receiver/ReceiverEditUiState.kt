package com.afternote.feature.setting.presentation.receiver

import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.UiState

internal data class ReceiverEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    /** 저장을 마쳤다는 사실. 소비되는 [pendingEvent] 와 달리 되돌리지 않아, 화면이 닫히는 동안에도 재제출을 막는다. */
    val isSaved: Boolean = false,
    val receiver: ReceiverDetail? = null,
    val errorMessage: UiText? = null,
    val pendingEvent: ReceiverEditEvent? = null,
) : UiState {
    val isSaveLocked: Boolean get() = isSaving || isSaved
}

internal sealed interface ReceiverEditEvent {
    data object EditSuccess : ReceiverEditEvent
}
