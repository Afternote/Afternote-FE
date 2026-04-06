package com.afternote.feature.afternote.presentation.author.detail.model

sealed interface AfternoteDetailEvent {
    data class LoadDetail(
        val afternoteId: Long,
    ) : AfternoteDetailEvent

    data class Delete(
        val afternoteId: Long,
    ) : AfternoteDetailEvent

    data object DeleteResultConsumed : AfternoteDetailEvent
}
