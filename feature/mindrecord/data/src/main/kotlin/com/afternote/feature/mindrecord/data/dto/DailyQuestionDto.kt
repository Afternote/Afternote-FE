@file:OptIn(ExperimentalSerializationApi::class)

package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class DailyQuestionCreateRequestDto(
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("questionId") val questionId: Long,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DailyQuestionUpdateRequestDto(
    @SerialName("content") val content: String? = null,
    @SerialName("isDraft") val isDraft: Boolean? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("questionId") val questionId: Long? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DailyQuestionListItemDto(
    // 서버는 사용자별 답변 레코드 ID 를 `userDailyQuestionId` 로 내려준다.
    // 도메인에서는 그대로 `dailyQuestionId` 로 받지만 와이어 키는 다름에 주의.
    // 노션 명세 예시는 `dailyQuestionId` 키를 사용 — 어느 쪽이 오더라도 파싱되도록 대체 키 허용.
    @SerialName("userDailyQuestionId")
    @JsonNames("dailyQuestionId")
    val dailyQuestionId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class TodayDailyQuestionDto(
    @SerialName("questionId") val questionId: Long,
    @SerialName("day") val day: Int,
    @SerialName("content") val content: String,
    @SerialName("answered") val isAnswered: Boolean,
)
