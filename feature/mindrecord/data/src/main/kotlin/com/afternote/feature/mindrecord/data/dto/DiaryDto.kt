package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TodayMood {
    @SerialName("HAPPY") HAPPY,

    @SerialName("SOSO") SOSO,

    @SerialName("SAD") SAD,
}

@Serializable
data class DiaryCreateRequest(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("todayMood") val todayMood: TodayMood,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DiaryUpdateRequest(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("todayMood") val todayMood: TodayMood,
    @SerialName("date") val date: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DiaryListItem(
    @SerialName("diaryId") val diaryId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("todayMood") val todayMood: TodayMood,
)
