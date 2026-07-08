package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.DiaryCreateRequest
import com.afternote.feature.mindrecord.data.dto.DiaryListItem
import com.afternote.feature.mindrecord.data.dto.DiaryListResponse
import com.afternote.feature.mindrecord.data.dto.DiaryUpdateRequest
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.MindRecordReceiver
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.data.dto.TodayMood as TodayMoodDto

fun DiaryListItem.toDomain(): Diary =
    Diary(
        diaryId = diaryId,
        title = title,
        content = content,
        date = date,
        createdAt = createdAt,
        isDraft = isDraft,
        todayMood = todayMood.toDomain(),
        imageUrl = imageUrl,
    )

fun DiaryListResponse.toDomain(): DiaryList =
    DiaryList(
        diaries = diaries.map { it.toDomain() },
        monthDiaryCount = monthDiaryCount,
        weeklyDominantMood = weeklyDominantMood?.toDomain(),
        receivers = receivers.map { MindRecordReceiver(receiverId = it.receiverId, name = it.name) },
    )

fun TodayMoodDto.toDomain(): TodayMood =
    when (this) {
        TodayMoodDto.HAPPY -> TodayMood.HAPPY
        TodayMoodDto.SOSO -> TodayMood.SOSO
        TodayMoodDto.SAD -> TodayMood.SAD
    }

fun TodayMood.toDto(): TodayMoodDto =
    when (this) {
        TodayMood.HAPPY -> TodayMoodDto.HAPPY
        TodayMood.SOSO -> TodayMoodDto.SOSO
        TodayMood.SAD -> TodayMoodDto.SAD
    }

fun DiaryCreatePayload.toRequest(): DiaryCreateRequest =
    DiaryCreateRequest(
        title = title,
        content = content,
        isDraft = isDraft,
        todayMood = todayMood.toDto(),
        receiverIds = receiverIds,
    )

fun DiaryUpdatePayload.toRequest(): DiaryUpdateRequest =
    DiaryUpdateRequest(
        title = title,
        content = content,
        isDraft = isDraft,
        todayMood = todayMood?.toDto(),
        receiverIds = receiverIds,
    )
