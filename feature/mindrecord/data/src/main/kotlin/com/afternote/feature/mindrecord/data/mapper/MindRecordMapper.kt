package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.MindRecordDetailResponse
import com.afternote.feature.mindrecord.data.dto.MindRecordListItem
import com.afternote.feature.mindrecord.data.dto.MindRecordListResponse
import com.afternote.feature.mindrecord.data.dto.MindRecordMedia as MindRecordMediaDto
import com.afternote.feature.mindrecord.data.dto.MindRecordMediaType as MindRecordMediaTypeDto
import com.afternote.feature.mindrecord.data.dto.MindRecordType as MindRecordTypeDto
import com.afternote.feature.mindrecord.domain.model.MindRecordDetail
import com.afternote.feature.mindrecord.domain.model.MindRecordList
import com.afternote.feature.mindrecord.domain.model.MindRecordMedia
import com.afternote.feature.mindrecord.domain.model.MindRecordMediaType
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType

fun MindRecordListResponse.toDomain(): MindRecordList =
    MindRecordList(
        mindRecords = mindRecords.map { it.toDomain() },
        totalCount = totalCount,
    )

fun MindRecordListItem.toDomain(): MindRecordSummary =
    MindRecordSummary(
        id = id,
        type = type.toDomain(),
        title = title,
        recordDate = recordDate,
        isDraft = isDraft,
        senderName = senderName,
        createdAt = createdAt,
    )

fun MindRecordDetailResponse.toDomain(): MindRecordDetail =
    MindRecordDetail(
        id = id,
        type = type.toDomain(),
        title = title,
        recordDate = recordDate,
        content = content,
        senderName = senderName,
        createdAt = createdAt,
        questionId = questionId,
        questionContent = questionContent,
        category = category,
        mediaList = imageList.map { it.toDomain() },
    )

fun MindRecordMediaDto.toDomain(): MindRecordMedia =
    MindRecordMedia(
        id = id,
        mediaType = mediaType.toDomain(),
        imageUrl = imageUrl,
    )

fun MindRecordTypeDto.toDomain(): MindRecordType =
    when (this) {
        MindRecordTypeDto.DAILY_QUESTION -> MindRecordType.DAILY_QUESTION
        MindRecordTypeDto.DIARY -> MindRecordType.DIARY
        MindRecordTypeDto.DEEP_THOUGHT -> MindRecordType.DEEP_THOUGHT
    }

fun MindRecordMediaTypeDto.toDomain(): MindRecordMediaType =
    when (this) {
        MindRecordMediaTypeDto.IMAGE -> MindRecordMediaType.IMAGE
        MindRecordMediaTypeDto.VIDEO -> MindRecordMediaType.VIDEO
    }
