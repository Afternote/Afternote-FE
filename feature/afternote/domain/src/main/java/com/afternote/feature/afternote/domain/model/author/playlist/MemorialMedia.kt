package com.afternote.feature.afternote.domain.model.author.playlist

/** 추억 사진·장례식 영상. 셋 다 미등록일 수 있어 개별 nullable 이다. */
data class MemorialMedia(
    val photoUrl: String?,
    val videoUrl: String?,
    val thumbnailUrl: String?,
)

/** 추억 노트에 담긴 개별 곡. */
data class DetailSong(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)
