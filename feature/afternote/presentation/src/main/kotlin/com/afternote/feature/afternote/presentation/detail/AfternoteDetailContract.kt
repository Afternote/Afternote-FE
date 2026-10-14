package com.afternote.feature.afternote.presentation.detail

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface AfternoteDetailIntent : MviIntent {
    data object Retry : AfternoteDetailIntent

    data object RefreshOnReturn : AfternoteDetailIntent

    data object Delete : AfternoteDetailIntent

    data object ConsumeDeleteResult : AfternoteDetailIntent
}

internal sealed interface AfternoteDetailReducerEvent : ReducerEvent {
    data object Loading : AfternoteDetailReducerEvent

    data class ContentLoaded(
        val id: Long,
        val content: DetailContentUiModel,
        val authorName: String,
    ) : AfternoteDetailReducerEvent

    data class LoadFailed(
        val keepsContent: Boolean,
    ) : AfternoteDetailReducerEvent

    data object DeleteStarted : AfternoteDetailReducerEvent

    data class DeleteFinished(
        val result: AfternoteDetailDeleteResult,
    ) : AfternoteDetailReducerEvent

    data object DeleteResultConsumed : AfternoteDetailReducerEvent

    data class AuthorNameChanged(
        val name: String,
    ) : AfternoteDetailReducerEvent
}
