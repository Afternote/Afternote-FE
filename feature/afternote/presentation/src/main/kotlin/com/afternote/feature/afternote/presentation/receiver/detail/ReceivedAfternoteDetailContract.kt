package com.afternote.feature.afternote.presentation.receiver.detail

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ReceivedAfternoteDetailIntent : MviIntent {
    data object Retry : ReceivedAfternoteDetailIntent

    data object RefreshOnReturn : ReceivedAfternoteDetailIntent
}

internal sealed interface ReceivedAfternoteDetailReducerEvent : ReducerEvent {
    data object Loading : ReceivedAfternoteDetailReducerEvent

    data class ContentLoaded(
        val id: Long,
        val content: ReceivedDetailContentUiModel,
    ) : ReceivedAfternoteDetailReducerEvent

    data class LoadFailed(
        val keepsContent: Boolean,
    ) : ReceivedAfternoteDetailReducerEvent
}
