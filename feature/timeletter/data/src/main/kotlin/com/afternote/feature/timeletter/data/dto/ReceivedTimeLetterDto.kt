package com.afternote.feature.timeletter.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceivedTimeLetterDto(
    @SerialName("id") val id: Long,
    @SerialName("timeLetterReceiverId") val timeLetterReceiverId: Long,
    @SerialName("title") val title: String?,
    @SerialName("blocks") val blocks: List<TimeLetterBlockDto>,
    @SerialName("sendAt") val sendAt: String?,
    @SerialName("status") val status: TimeLetterStatusDto,
    @SerialName("senderName") val senderName: String?,
    @SerialName("deliveredAt") val deliveredAt: String?,
    @SerialName("createdAt") val createdAt: String?,
    @SerialName("isRead") val isRead: Boolean?,
)

@Serializable
data class ReceivedTimeLetterListDto(
    @SerialName("timeLetters") val timeLetters: List<ReceivedTimeLetterDto>,
    @SerialName("totalCount") val totalCount: Int,
)
