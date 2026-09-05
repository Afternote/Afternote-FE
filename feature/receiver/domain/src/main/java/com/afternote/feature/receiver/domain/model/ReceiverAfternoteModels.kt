package com.afternote.feature.receiver.domain.model

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

data class AfterNotesListResult(
    val items: List<AfterNoteListItem>,
    val totalCount: Int,
)

data class AfterNoteListItem(
    val id: Long,
    val serviceName: String,
    val type: AfternoteType,
    val lastUpdatedAt: String?,
)

data class ReceivedExportBundle(
    val payloadJson: String = "{}",
)

data class ReceivedAfternoteDetail(
    val type: AfternoteType,
    val serviceName: String,
    val senderName: String,
    val createdAt: String? = null,
    val processingMethods: List<String> = emptyList(),
    val leaveMessageBlocks: List<LeaveMessageBlock> = emptyList(),
    val playlist: ReceivedPlaylistDetail? = null,
    val credentials: ReceivedAccountCredentials? = null,
)

data class ReceivedAccountCredentials(
    val id: String?,
    val password: String?,
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
