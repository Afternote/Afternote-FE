package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.DailyQuestionCreateRequest
import com.afternote.feature.mindrecord.data.dto.DailyQuestionListItem
import com.afternote.feature.mindrecord.data.dto.DailyQuestionUpdateRequest
import com.afternote.feature.mindrecord.data.dto.TodayDailyQuestionResponse
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion

fun DailyQuestionListItem.toDomain(): DailyQuestion =
    DailyQuestion(
        dailyQuestionId = dailyQuestionId,
        title = title,
        content = content,
        createdAt = createdAt,
        imageUrl = imageUrl,
    )

fun TodayDailyQuestionResponse.toDomain(): TodayDailyQuestion =
    TodayDailyQuestion(
        questionId = questionId,
        day = day,
        content = content,
        isAnswered = isAnswered,
    )

fun DailyQuestionCreatePayload.toRequest(): DailyQuestionCreateRequest =
    DailyQuestionCreateRequest(
        content = content,
        isDraft = isDraft,
        questionId = questionId,
        imageUrl = imageUrl,
    )

fun DailyQuestionUpdatePayload.toRequest(): DailyQuestionUpdateRequest =
    DailyQuestionUpdateRequest(
        content = content,
        isDraft = isDraft,
        date = date,
        questionId = questionId,
        imageUrl = imageUrl,
    )
