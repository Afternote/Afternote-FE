package com.afternote.feature.timeletter.data.mapper

import com.afternote.feature.timeletter.data.dto.TimeLetterBlockDto
import com.afternote.feature.timeletter.data.dto.TimeLetterBlockRequestDto
import com.afternote.feature.timeletter.data.dto.TimeLetterBlockTypeDto
import com.afternote.feature.timeletter.data.dto.TimeLetterDeliveryModeDto
import com.afternote.feature.timeletter.data.dto.TimeLetterDto
import com.afternote.feature.timeletter.data.dto.TimeLetterListDto
import com.afternote.feature.timeletter.data.dto.TimeLetterStatusDto
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
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

fun TimeLetterDeliveryMode.toDto(): TimeLetterDeliveryModeDto =
    when (this) {
        TimeLetterDeliveryMode.DATE -> TimeLetterDeliveryModeDto.DATE
        TimeLetterDeliveryMode.POST_DEATH -> TimeLetterDeliveryModeDto.POST_DEATH
    }

fun TimeLetterBlockTypeDto.toDomain(): TimeLetterBlockType =
    when (this) {
        TimeLetterBlockTypeDto.TEXT -> TimeLetterBlockType.TEXT
        TimeLetterBlockTypeDto.IMAGE -> TimeLetterBlockType.IMAGE
        TimeLetterBlockTypeDto.AUDIO -> TimeLetterBlockType.AUDIO
        TimeLetterBlockTypeDto.FILE -> TimeLetterBlockType.FILE
        TimeLetterBlockTypeDto.LINK -> TimeLetterBlockType.LINK
    }

fun TimeLetterBlockDto.toDomain(): TimeLetterBlock =
    TimeLetterBlock(
        id = id,
        blockType = blockType.toDomain(),
        blockOrder = blockOrder,
        textContent = textContent,
        url = url,
        mimeType = mimeType,
    )

fun TimeLetterDto.toDomain(): TimeLetter =
    TimeLetter(
        id = id,
        title = title,
        sendAt = sendAt,
        deliveredAt = null,
        status = status.toDomain(),
        blocks = blocks.map { it.toDomain() },
        receiverIds = receiverIds,
    )

fun TimeLetterListDto.toDomain(): TimeLetterList =
    TimeLetterList(
        timeLetters = timeLetters.map { it.toDomain() },
        totalCount = totalCount,
    )

fun NewTimeLetterBlock.toDto(): TimeLetterBlockRequestDto =
    TimeLetterBlockRequestDto(
        blockType =
            when (blockType) {
                TimeLetterBlockType.TEXT -> TimeLetterBlockTypeDto.TEXT
                TimeLetterBlockType.IMAGE -> TimeLetterBlockTypeDto.IMAGE
                TimeLetterBlockType.AUDIO -> TimeLetterBlockTypeDto.AUDIO
                TimeLetterBlockType.FILE -> TimeLetterBlockTypeDto.FILE
                TimeLetterBlockType.LINK -> TimeLetterBlockTypeDto.LINK
            },
        blockOrder = blockOrder,
        textContent = textContent,
        url = url,
        mimeType = mimeType,
    )
