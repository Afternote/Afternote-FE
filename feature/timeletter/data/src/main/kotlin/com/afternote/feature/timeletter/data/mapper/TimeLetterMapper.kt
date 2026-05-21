package com.afternote.feature.timeletter.data.mapper

import com.afternote.feature.timeletter.data.dto.TimeLetterBlockResponseDto
import com.afternote.feature.timeletter.data.dto.TimeLetterBlockTypeDto
import com.afternote.feature.timeletter.data.dto.TimeLetterListResponseDto
import com.afternote.feature.timeletter.data.dto.TimeLetterResponseDto
import com.afternote.feature.timeletter.data.dto.TimeLetterStatusDto
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus

fun TimeLetterStatusDto.toDomain(): TimeLetterStatus =
    when (this) {
        TimeLetterStatusDto.DRAFT -> TimeLetterStatus.DRAFT
        TimeLetterStatusDto.SCHEDULED -> TimeLetterStatus.SCHEDULED
        TimeLetterStatusDto.SENT -> TimeLetterStatus.SENT
    }

fun TimeLetterStatus.toDto(): TimeLetterStatusDto =
    when (this) {
        TimeLetterStatus.DRAFT -> TimeLetterStatusDto.DRAFT
        TimeLetterStatus.SCHEDULED -> TimeLetterStatusDto.SCHEDULED
        TimeLetterStatus.SENT -> TimeLetterStatusDto.SENT
    }

fun TimeLetterBlockTypeDto.toDomain(): TimeLetterBlockType =
    when (this) {
        TimeLetterBlockTypeDto.TEXT -> TimeLetterBlockType.TEXT
        TimeLetterBlockTypeDto.IMAGE -> TimeLetterBlockType.IMAGE
        TimeLetterBlockTypeDto.AUDIO -> TimeLetterBlockType.AUDIO
        TimeLetterBlockTypeDto.FILE -> TimeLetterBlockType.FILE
        TimeLetterBlockTypeDto.LINK -> TimeLetterBlockType.LINK
    }

fun TimeLetterBlockResponseDto.toDomain(): TimeLetterBlock =
    TimeLetterBlock(
        id = id,
        blockType = blockType.toDomain(),
        blockOrder = blockOrder,
        textContent = textContent,
        url = url,
        mimeType = mimeType,
    )

fun TimeLetterResponseDto.toDomain(): TimeLetter =
    TimeLetter(
        id = id,
        title = title,
        sendAt = sendAt,
        deliveredAt = deliveredAt,
        status = status.toDomain(),
        blocks = blocks.map { it.toDomain() },
        receiverIds = receiverIds,
    )

fun TimeLetterListResponseDto.toDomain(): TimeLetterList =
    TimeLetterList(
        timeLetters = timeLetters.map { it.toDomain() },
        totalCount = totalCount,
    )
