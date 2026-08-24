package com.afternote.feature.receiver.data.mapper

import com.afternote.feature.afternote.data.mapper.afternoteTypeFromServerCategory
import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult

/** 서버가 모르는 카테고리를 보내면 [AfterNoteListItem.type] 은 null 이다. */
fun ReceivedAfternoteDto.toDomain(): AfterNoteListItem =
    AfterNoteListItem(
        id = id,
        serviceName = title,
        type = category?.let(::afternoteTypeFromServerCategory),
        lastUpdatedAt = createdAt?.let { formatDateFromServer(it) },
    )

fun List<ReceivedAfternoteDto>.toReceiverDomainList(): List<AfterNoteListItem> = map { it.toDomain() }

fun ReceivedAfternoteListDto.toDomainResult(): AfterNotesListResult =
    AfterNotesListResult(
        items = afternotes.toReceiverDomainList(),
        totalCount = totalCount,
    )
