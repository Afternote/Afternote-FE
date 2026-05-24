package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyQuestionCreateRequest(
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("questionId") val questionId: Long,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DailyQuestionUpdateRequest(
    @SerialName("content") val content: String? = null,
    @SerialName("isDraft") val isDraft: Boolean? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("questionId") val questionId: Long? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DailyQuestionListItem(
    // 서버는 사용자별 답변 레코드 ID 를 `userDailyQuestionId` 로 내려준다.
    // 도메인에서는 그대로 `dailyQuestionId` 로 받지만 와이어 키는 다름에 주의.
    @SerialName("userDailyQuestionId") val dailyQuestionId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class TodayDailyQuestionResponse(
    @SerialName("questionId") val questionId: Long,
    @SerialName("day") val day: Int,
    @SerialName("content") val content: String,
    @SerialName("answered") val isAnswered: Boolean,
)
