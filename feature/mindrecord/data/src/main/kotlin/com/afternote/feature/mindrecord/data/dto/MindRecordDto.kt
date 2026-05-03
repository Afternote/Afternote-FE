package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MindRecordType {
    @SerialName("DAILY_QUESTION") DAILY_QUESTION,

    @SerialName("DIARY") DIARY,

    @SerialName("DEEP_THOUGHT") DEEP_THOUGHT,
}

@Serializable
enum class MindRecordMediaType {
    @SerialName("IMAGE") IMAGE,

    @SerialName("VIDEO") VIDEO,
}

@Serializable
data class MindRecordListResponse(
    @SerialName("mindRecords") val mindRecords: List<MindRecordListItem> = emptyList(),
    @SerialName("totalCount") val totalCount: Int,
)

@Serializable
data class MindRecordListItem(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: MindRecordType,
    @SerialName("title") val title: String,
    @SerialName("recordDate") val recordDate: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("senderName") val senderName: String,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class MindRecordDetailResponse(
    @SerialName("id") val id: Long,
    @SerialName("type") val type: MindRecordType,
    @SerialName("title") val title: String,
    @SerialName("recordDate") val recordDate: String,
    @SerialName("content") val content: String,
    @SerialName("questionId") val questionId: Long? = null,
    @SerialName("questionContent") val questionContent: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("senderName") val senderName: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("imageList") val imageList: List<MindRecordMedia> = emptyList(),
)

@Serializable
data class MindRecordMedia(
    @SerialName("id") val id: Long,
    @SerialName("mediaType") val mediaType: MindRecordMediaType,
    @SerialName("imageUrl") val imageUrl: String,
)
