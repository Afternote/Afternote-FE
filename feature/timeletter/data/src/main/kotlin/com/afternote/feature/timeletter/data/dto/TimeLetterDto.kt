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
    // 날짜 지정 발송
    @SerialName("DATE")
    DATE,

    // 사후(死後) 전달
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
data class TimeLetterBlockRequest(
    @SerialName("blockType") val blockType: TimeLetterBlockTypeDto,
    @SerialName("blockOrder") val blockOrder: Int,
    @SerialName("textContent") val textContent: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("mimeType") val mimeType: String? = null,
)

@Serializable
data class TimeLetterBlockResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("blockType") val blockType: TimeLetterBlockTypeDto,
    @SerialName("blockOrder") val blockOrder: Int,
    @SerialName("textContent") val textContent: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("mimeType") val mimeType: String? = null,
)

@Serializable
data class TimeLetterCreateRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("deliveryMode") val deliveryMode: TimeLetterDeliveryModeDto? = null,
    @SerialName("status") val status: TimeLetterStatusDto,
    @SerialName("blocks") val blocks: List<TimeLetterBlockRequest> = emptyList(),
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
)

@Serializable
data class TimeLetterUpdateRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("deliveryMode") val deliveryMode: TimeLetterDeliveryModeDto? = null,
    @SerialName("status") val status: TimeLetterStatusDto? = null,
    @SerialName("blocks") val blocks: List<TimeLetterBlockRequest> = emptyList(),
)

@Serializable
data class TimeLetterDeleteRequest(
    @SerialName("timeLetterIds") val timeLetterIds: List<Long>,
)

@Serializable
data class TimeLetterResponseDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String? = null,
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("deliveredAt") val deliveredAt: String? = null,
    @SerialName("status") val status: TimeLetterStatusDto,
    @SerialName("blocks") val blocks: List<TimeLetterBlockResponseDto> = emptyList(),
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
)

@Serializable
data class TimeLetterListResponseDto(
    @SerialName("timeLetters") val timeLetters: List<TimeLetterResponseDto>,
    @SerialName("totalCount") val totalCount: Int,
)
