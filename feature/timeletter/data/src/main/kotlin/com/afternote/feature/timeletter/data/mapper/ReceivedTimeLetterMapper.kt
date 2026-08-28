package com.afternote.feature.timeletter.data.mapper

import com.afternote.feature.timeletter.data.dto.ReceivedTimeLetterDto
import com.afternote.feature.timeletter.data.dto.ReceivedTimeLetterListDto
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList

fun ReceivedTimeLetterDto.toDomain(): ReceivedTimeLetter =
    ReceivedTimeLetter(
        id = id,
        timeLetterReceiverId = timeLetterReceiverId,
        title = title,
        blocks = blocks.map { it.toDomain() },
        sendAt = sendAt,
        status = status.toDomain(),
        senderName = senderName,
        deliveredAt = deliveredAt,
        createdAt = createdAt,
        isRead = isRead ?: false,
    )

fun ReceivedTimeLetterListDto.toDomain(): ReceivedTimeLetterList =
    ReceivedTimeLetterList(
        timeLetters = timeLetters.map { it.toDomain() },
        totalCount = totalCount,
    )
