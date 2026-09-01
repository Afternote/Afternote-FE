package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.DailyQuestionCreateRequestDto
import com.afternote.feature.mindrecord.data.dto.DailyQuestionListItemDto
import com.afternote.feature.mindrecord.data.dto.DailyQuestionUpdateRequestDto
import com.afternote.feature.mindrecord.data.dto.TodayDailyQuestionDto
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion

fun DailyQuestionListItemDto.toDomain(): DailyQuestion =
    DailyQuestion(
        dailyQuestionId = dailyQuestionId,
        title = title,
        content = content,
        createdAt = createdAt,
        isDraft = isDraft,
        receiverNames = receivers.map { it.name },
    )

fun TodayDailyQuestionDto.toDomain(): TodayDailyQuestion =
    TodayDailyQuestion(
        questionId = questionId,
        day = day,
        content = content,
        isAnswered = isAnswered,
        isDraft = isDraft,
    )

fun DailyQuestionCreatePayload.toRequest(): DailyQuestionCreateRequestDto =
    DailyQuestionCreateRequestDto(
        content = content,
        isDraft = isDraft,
        questionId = questionId,
    )

fun DailyQuestionUpdatePayload.toRequest(): DailyQuestionUpdateRequestDto =
    DailyQuestionUpdateRequestDto(
        content = content,
        isDraft = isDraft,
        date = date,
        questionId = questionId,
    )
