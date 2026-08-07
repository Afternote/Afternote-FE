package com.afternote.feature.timeletter.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TimeLetterStatusDto {
    @SerialName("DRAFT")
    DRAFT,

    @SerialName("SCHEDULED")
    SCHEDULED,

    @SerialName("SENT")
    SENT,
}

@Serializable
enum class TimeLetterDeliveryModeDto {
    @SerialName("DATE")
    DATE,

    @SerialName("POST_DEATH")
    POST_DEATH,
}

@Serializable
enum class TimeLetterBlockTypeDto {
    @SerialName("TEXT")
    TEXT,

    @SerialName("IMAGE")
    IMAGE,

    @SerialName("AUDIO")
    AUDIO,

    @SerialName("FILE")
    FILE,

    @SerialName("LINK")
    LINK,
}

@Serializable
data class TimeLetterBlockRequestDto(
    @SerialName("blockType") val blockType: TimeLetterBlockTypeDto,
    @SerialName("blockOrder") val blockOrder: Int,
    @SerialName("textContent") val textContent: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("mimeType") val mimeType: String? = null,
)

@Serializable
data class TimeLetterBlockDto(
    @SerialName("id") val id: Long,
    @SerialName("blockType") val blockType: TimeLetterBlockTypeDto,
    @SerialName("blockOrder") val blockOrder: Int,
    @SerialName("textContent") val textContent: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("mimeType") val mimeType: String? = null,
)

@Serializable
data class TimeLetterCreateRequestDto(
    @SerialName("title") val title: String? = null,
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("deliveryMode") val deliveryMode: TimeLetterDeliveryModeDto,
    @SerialName("status") val status: TimeLetterStatusDto,
    @SerialName("blocks") val blocks: List<TimeLetterBlockRequestDto> = emptyList(),
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
)

@Serializable
data class TimeLetterUpdateRequestDto(
    @SerialName("title") val title: String? = null,
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("deliveryMode") val deliveryMode: TimeLetterDeliveryModeDto? = null,
    @SerialName("status") val status: TimeLetterStatusDto? = null,
    @SerialName("blocks") val blocks: List<TimeLetterBlockRequestDto> = emptyList(),
)

@Serializable
data class TimeLetterDeleteRequestDto(
    @SerialName("timeLetterIds") val timeLetterIds: List<Long>,
)

@Serializable
data class TimeLetterDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String? = null,
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("deliveredAt") val deliveredAt: String? = null,
    @SerialName("status") val status: TimeLetterStatusDto,
    @SerialName("blocks") val blocks: List<TimeLetterBlockDto> = emptyList(),
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
)

@Serializable
data class TimeLetterListDto(
    @SerialName("timeLetters") val timeLetters: List<TimeLetterDto>,
    @SerialName("totalCount") val totalCount: Int,
)
