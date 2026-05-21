package com.afternote.feature.timeletter.data.mapper

import com.afternote.feature.timeletter.data.dto.TimeLetterListResponseDto
import com.afternote.feature.timeletter.data.dto.TimeLetterMediaRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterMediaResponseDto
import com.afternote.feature.timeletter.data.dto.TimeLetterMediaTypeDto
import com.afternote.feature.timeletter.data.dto.TimeLetterResponseDto
import com.afternote.feature.timeletter.data.dto.TimeLetterStatusDto
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterMedia
import com.afternote.feature.timeletter.domain.model.TimeLetterMediaType
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus

// ========================================
// Enum Mapper
// ========================================

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

fun TimeLetterMediaTypeDto.toDomain(): TimeLetterMediaType =
    when (this) {
        TimeLetterMediaTypeDto.IMAGE -> TimeLetterMediaType.IMAGE
        TimeLetterMediaTypeDto.VIDEO -> TimeLetterMediaType.VIDEO
        TimeLetterMediaTypeDto.AUDIO -> TimeLetterMediaType.AUDIO
        TimeLetterMediaTypeDto.DOCUMENT -> TimeLetterMediaType.DOCUMENT
    }

fun TimeLetterMediaType.toDto(): TimeLetterMediaTypeDto =
    when (this) {
        TimeLetterMediaType.IMAGE -> TimeLetterMediaTypeDto.IMAGE
        TimeLetterMediaType.VIDEO -> TimeLetterMediaTypeDto.VIDEO
        TimeLetterMediaType.AUDIO -> TimeLetterMediaTypeDto.AUDIO
        TimeLetterMediaType.DOCUMENT -> TimeLetterMediaTypeDto.DOCUMENT
    }

// ========================================
// Response Mapper (DTO → Domain)
// ========================================

fun TimeLetterResponseDto.toDomain(): TimeLetter =
    TimeLetter(
        id = id,
        title = title,
        content = content,
        sendAt = sendAt,
        status = status.toDomain(),
        mediaList = mediaList.map { it.toDomain() },
        receiverIds = receiverIds,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun TimeLetterMediaResponseDto.toDomain(): TimeLetterMedia =
    TimeLetterMedia(
        id = id,
        mediaType = mediaType.toDomain(),
        mediaUrl = mediaUrl,
    )

fun TimeLetterListResponseDto.toDomain(): TimeLetterList =
    TimeLetterList(
        timeLetters = timeLetters.map { it.toDomain() },
        totalCount = totalCount,
    )

// ========================================
// Request Mapper (Domain → DTO)
// ========================================

fun TimeLetterMedia.toRequest(): TimeLetterMediaRequest =
    TimeLetterMediaRequest(
        mediaType = mediaType.toDto(),
        mediaUrl = mediaUrl,
    )
