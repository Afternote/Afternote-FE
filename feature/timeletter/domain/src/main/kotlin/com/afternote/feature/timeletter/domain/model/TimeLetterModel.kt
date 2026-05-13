package com.afternote.feature.timeletter.domain.model

data class TimeLetter(
    val id: Long,
    val title: String?,
    val content: String?,
    val sendAt: String?,
    val status: TimeLetterStatus,
    val mediaList: List<TimeLetterMedia>,
    val receiverIds: List<Long>,
    val createdAt: String?,
    val updatedAt: String?,
)

data class TimeLetterMedia(
    val id: Long,
    val mediaType: TimeLetterMediaType,
    val mediaUrl: String,
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

enum class TimeLetterMediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
}

