package com.afternote.feature.afternote.domain.model.author

sealed interface SaveAfternoteCommand {
    data class Create(
        val input: CreateAfternoteInput,
    ) : SaveAfternoteCommand

    data class Update(
        val id: Long,
        val payload: AfternoteUpdatePayload,
    ) : SaveAfternoteCommand
}
