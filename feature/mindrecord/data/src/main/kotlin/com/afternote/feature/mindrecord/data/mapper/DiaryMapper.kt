package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.DiaryCreateRequestDto
import com.afternote.feature.mindrecord.data.dto.DiaryListDto
import com.afternote.feature.mindrecord.data.dto.DiaryListItemDto
import com.afternote.feature.mindrecord.data.dto.DiaryUpdateRequestDto
import com.afternote.feature.mindrecord.data.dto.TodayMoodDto
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood

fun DiaryListItemDto.toDomain(): Diary =
    Diary(
        diaryId = diaryId,
        title = title,
        content = content,
        date = date,
        createdAt = createdAt,
        todayMood = todayMood.toDomain(),
        imageUrl = imageUrl,
        isDraft = isDraft,
        receiverNames = receivers.map { it.name },
    )

fun DiaryListDto.toDomain(): DiaryList =
    DiaryList(
        diaries = diaries.map { it.toDomain() },
        monthDiaryCount = monthDiaryCount,
        weeklyDominantMood = weeklyDominantMood?.toDomain(),
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

fun DiaryCreatePayload.toRequest(): DiaryCreateRequestDto =
    DiaryCreateRequestDto(
        title = title,
        content = content,
        isDraft = isDraft,
        todayMood = todayMood.toDto(),
        receiverIds = receiverIds,
        // LocalDate.toString() 이 ISO-8601 `yyyy-MM-dd` 다 — 서버 계약과 같은 형식이다.
        date = date.toString(),
    )

fun DiaryUpdatePayload.toRequest(): DiaryUpdateRequestDto =
    DiaryUpdateRequestDto(
        title = title,
        content = content,
        isDraft = isDraft,
        todayMood = todayMood.toDto(),
        receiverIds = receiverIds,
        date = date?.toString(),
    )
