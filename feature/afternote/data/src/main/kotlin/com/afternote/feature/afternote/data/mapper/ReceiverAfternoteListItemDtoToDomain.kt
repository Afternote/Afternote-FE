package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteDto
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItem

/** 서버가 모르는 카테고리를 보내면 [AfterNoteListItem.type] 은 null 이다. */
fun ReceivedAfternoteDto.toDomain(): AfterNoteListItem =
    AfterNoteListItem(
        id = id,
        title = title,
        type = category?.let(::afternoteTypeFromServerCategory),
        lastUpdatedAt = createdAt?.let { formatDateFromServer(it) },
    )

fun List<ReceivedAfternoteDto>.toReceiverDomainList(): List<AfterNoteListItem> = map { it.toDomain() }
