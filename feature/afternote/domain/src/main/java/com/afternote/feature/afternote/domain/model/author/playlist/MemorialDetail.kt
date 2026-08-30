package com.afternote.feature.afternote.domain.model.author.playlist

/**
 * 추억 노트(MEMORIAL) 카테고리의 상세 — 곡 목록과 사진·영상.
 */
data class MemorialDetail(
    val songs: List<DetailSong>,
    val media: MemorialMedia,
)

/** 추억 사진·장례식 영상·추모 음성. 넷 다 미등록일 수 있어 개별 nullable 이다. */
data class MemorialMedia(
    val photoUrl: String?,
    val videoUrl: String?,
    val thumbnailUrl: String?,
    /** 추모 음성 URL (#1118). 서버 `playlist.memorialAudioUrl`. */
    val audioUrl: String?,
)

/** 추억 플레이리스트의 개별 곡. */
data class DetailSong(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)
