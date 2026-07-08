package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class MindRecordReceiverDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("name") val name: String,
)

@Serializable
data class DailyQuestionCreateRequest(
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("questionId") val questionId: Long,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
)

@Serializable
data class DailyQuestionUpdateRequest(
    @SerialName("content") val content: String? = null,
    @SerialName("isDraft") val isDraft: Boolean? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("questionId") val questionId: Long? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DailyQuestionListItem(
    // 서버는 사용자별 답변 레코드 ID 를 `userDailyQuestionId` 로 내려준다.
    // 도메인에서는 그대로 `dailyQuestionId` 로 받지만 와이어 키는 다름에 주의.
    // 명세서 예시는 `dailyQuestionId` 키를 쓰므로 두 키 모두 허용.
    @SerialName("userDailyQuestionId")
    @JsonNames("dailyQuestionId")
    val dailyQuestionId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("receivers") val receivers: List<MindRecordReceiverDto> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TodayDailyQuestionResponse(
    @SerialName("questionId") val questionId: Long,
    // 명세서 응답 예시에는 없는 필드 — 서버가 생략해도 디코딩되도록 기본값 유지.
    @SerialName("day") val day: Int = 0,
    @SerialName("content") val content: String,
    // 명세서 예시 키는 `isAnswered`, 실서버는 `answered` — 두 키 모두 허용.
    @SerialName("answered")
    @JsonNames("isAnswered")
    val isAnswered: Boolean,
)
