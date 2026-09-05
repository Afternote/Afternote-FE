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
    // 공개 전 미리보기에선 읽음 여부가 아직 정해지지 않아 서버가 명시적 null을 보낼 수 있다(#790).
    @SerialName("isRead") val isRead: Boolean?,
)

@Serializable
data class ReceivedTimeLetterListDto(
    @SerialName("timeLetters") val timeLetters: List<ReceivedTimeLetterDto>,
    @SerialName("totalCount") val totalCount: Int,
)
