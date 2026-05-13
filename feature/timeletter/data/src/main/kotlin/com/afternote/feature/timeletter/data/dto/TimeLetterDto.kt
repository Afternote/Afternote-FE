package com.afternote.feature.timeletter.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TimeLetterStatusDto {
    @SerialName("DRAFT") DRAFT,
    @SerialName("SCHEDULED") SCHEDULED,
    @SerialName("SENT") SENT,
}

@Serializable
enum class TimeLetterMediaTypeDto {
    @SerialName("IMAGE") IMAGE,
    @SerialName("VIDEO") VIDEO,
    @SerialName("AUDIO") AUDIO,
    @SerialName("DOCUMENT") DOCUMENT,
}

@Serializable
data class TimeLetterCreateRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("status") val status: TimeLetterStatusDto,
    @SerialName("mediaList") val mediaList: List<TimeLetterMediaRequest> = emptyList(),
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
    @SerialName("deliveredAt") val deliveredAt: String? = null,
)

@Serializable
data class TimeLetterUpdateRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("status") val status: TimeLetterStatusDto? = null,
    @SerialName("mediaList") val mediaList: List<TimeLetterMediaRequest> = emptyList(),
)

@Serializable
data class TimeLetterDeleteRequest(
    @SerialName("timeLetterIds") val timeLetterIds: List<Long>,
)

@Serializable
data class TimeLetterMediaRequest(
    @SerialName("mediaType") val mediaType: TimeLetterMediaTypeDto,
    @SerialName("mediaUrl") val mediaUrl: String,
)

@Serializable
data class TimeLetterResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("status") val status: TimeLetterStatusDto,
    @SerialName("mediaList") val mediaList: List<TimeLetterMediaResponseDto> = emptyList(),
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
data class TimeLetterMediaResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("mediaType") val mediaType: TimeLetterMediaTypeDto,
    @SerialName("mediaUrl") val mediaUrl: String,
)

@Serializable
data class TimeLetterListResponseDto(
    @SerialName("timeLetters") val timeLetters: List<TimeLetterResponseDto>,
    @SerialName("totalCount") val totalCount: Int,
)
