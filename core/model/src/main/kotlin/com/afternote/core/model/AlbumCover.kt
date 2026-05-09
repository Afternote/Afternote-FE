package com.afternote.core.model

/**
 * 플레이리스트 앨범 커버 표시용 (작성·수신·상세 공통).
 */
data class AlbumCover(
    val id: String,
    val imageUrl: String? = null,
    val title: String? = null,
)
