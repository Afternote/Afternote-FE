package com.afternote.feature.timeletter.domain.model

data class ReceivedTimeLetter(
    val id: Long,
    val timeLetterReceiverId: Long,
    val title: String?,
    val blocks: List<TimeLetterBlock>,
    val sendAt: String?,
    val status: TimeLetterStatus,
    val senderName: String?,
    val deliveredAt: String?,
    val createdAt: String?,
    val isRead: Boolean,
) {
    val content: String?
        get() = blocks.firstOrNull { it.blockType == TimeLetterBlockType.TEXT }?.textContent
}

data class ReceivedTimeLetterList(
    val timeLetters: List<ReceivedTimeLetter>,
    val totalCount: Int,
)
