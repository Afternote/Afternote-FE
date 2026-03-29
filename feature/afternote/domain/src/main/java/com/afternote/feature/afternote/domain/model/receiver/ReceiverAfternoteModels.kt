package com.afternote.feature.afternote.domain.model.receiver

data class AfterNotesListResult(
    val items: List<AfterNoteListItemDto>,
    val totalCount: Int,
)

data class AfterNoteListItemDto(
    val id: Long,
    val title: String?,
    val sourceType: String?,
    val lastUpdatedAt: String?,
)

data class LoadCountResult(
    val totalCount: Int,
)

data class ReceivedExportBundle(
    val payloadJson: String = "{}",
)

data class ReceivedAfternoteDetail(
    val title: String? = null,
    val senderName: String? = null,
    val createdAt: String? = null,
    val category: String? = null,
    val processMethod: String? = null,
    val actions: List<String> = emptyList(),
    val leaveMessage: String? = null,
    val playlist: ReceivedPlaylistDetail? = null,
)

data class ReceivedPlaylistDetail(
    val songs: List<ReceivedPlaylistSong> = emptyList(),
    val atmosphere: String? = null,
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
)

data class ReceivedPlaylistSong(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)
