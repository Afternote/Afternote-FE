package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

data class CreateMemorialPayload(
    val title: String,
    val memorial: MemorialWritePayload,
    val leaveMessageBlocks: List<LeaveMessageBlock> = emptyList(),
    val receiverIds: List<Long> = emptyList(),
)

data class MemorialWritePayload(
    val memorialPhotoUrl: String? = null,
    val songs: List<MemorialSongPayload> = emptyList(),
    val memorialVideo: MemorialVideoPayload? = null,
    /** 추모 음성 URL (#1118). 서버 `playlist.memorialAudioUrl` — 추억 노트당 1개, mp3·m4a·wav. */
    val memorialAudioUrl: String? = null,
)

data class MemorialVideoPayload(
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
)

data class MemorialSongPayload(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)
