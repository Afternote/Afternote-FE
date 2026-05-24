package com.afternote.feature.timeletter.domain.model

data class TimeLetter(
    val id: Long,
    val title: String?,
    val sendAt: String?,
    val deliveredAt: String?,
    val status: TimeLetterStatus,
    val blocks: List<TimeLetterBlock>,
    val receiverIds: List<Long>,
) {
    val content: String?
        get() = blocks.firstOrNull { it.blockType == TimeLetterBlockType.TEXT }?.textContent
}

data class TimeLetterBlock(
    val id: Long,
    val blockType: TimeLetterBlockType,
    val blockOrder: Int,
    val textContent: String?,
    val url: String?,
    val mimeType: String?,
)

data class NewTimeLetterBlock(
    val blockType: TimeLetterBlockType,
    val blockOrder: Int,
    val textContent: String? = null,
    val url: String? = null,
    val mimeType: String? = null,
)

data class TimeLetterList(
    val timeLetters: List<TimeLetter>,
    val totalCount: Int,
)

enum class TimeLetterStatus {
    DRAFT,
    SCHEDULED,
    SENT,
}

enum class TimeLetterBlockType {
    TEXT,
    IMAGE,
    AUDIO,
    FILE,
    LINK,
}
