package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteDto
import com.afternote.feature.afternote.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItem
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult

fun ReceivedAfternoteDto.toDomain(): AfterNoteListItem =
    AfterNoteListItem(
        id = id,
        serviceName = title,
        type = categoryToAfternoteType(category),
        lastUpdatedAt = createdAt?.let { formatDateFromServer(it) },
    )

fun List<ReceivedAfternoteDto>.toReceiverDomainList(): List<AfterNoteListItem> = map { it.toDomain() }

fun ReceivedAfternoteListDto.toDomainResult(): AfterNotesListResult =
    AfterNotesListResult(
        items = afternotes.toReceiverDomainList(),
        totalCount = totalCount,
    )
