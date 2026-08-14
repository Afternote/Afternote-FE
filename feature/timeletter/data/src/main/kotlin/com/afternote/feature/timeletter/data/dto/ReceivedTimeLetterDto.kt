package com.afternote.feature.timeletter.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceivedTimeLetterDto(
    @SerialName("id") val id: Long,
    @SerialName("timeLetterReceiverId") val timeLetterReceiverId: Long,
    @SerialName("title") val title: String? = null,
    @SerialName("blocks") val blocks: List<TimeLetterBlockDto> = emptyList(),
    @SerialName("sendAt") val sendAt: String? = null,
    @SerialName("status") val status: TimeLetterStatusDto,
    @SerialName("senderName") val senderName: String? = null,
    @SerialName("deliveredAt") val deliveredAt: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("isRead") val isRead: Boolean,
)

@Serializable
data class ReceivedTimeLetterListDto(
    @SerialName("timeLetters") val timeLetters: List<ReceivedTimeLetterDto>,
    @SerialName("totalCount") val totalCount: Int,
)
