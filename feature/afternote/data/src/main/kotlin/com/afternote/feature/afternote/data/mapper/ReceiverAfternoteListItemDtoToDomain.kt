package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.ReceiverAfternoteListItem
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto

fun ReceiverAfternoteListItem.toDomain(): AfterNoteListItemDto =
    AfterNoteListItemDto(
        id = afternoteId,
        title = title,
        sourceType = category,
        lastUpdatedAt = updatedAt,
    )

fun List<ReceiverAfternoteListItem>.toReceiverDomainList(): List<AfterNoteListItemDto> = map { it.toDomain() }
