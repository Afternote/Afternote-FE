package com.afternote.feature.receiver.data.mapper

import com.afternote.feature.afternote.data.mapper.categoryToAfternoteType
import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult

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
