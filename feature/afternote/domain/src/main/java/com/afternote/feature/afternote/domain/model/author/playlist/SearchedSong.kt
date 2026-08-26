package com.afternote.feature.afternote.domain.model.author.playlist

/** 검색 API에 곡 ID가 없어 [selectionKey]는 화면 선택용으로 클라이언트가 생성한다. */
data class SearchedSong(
    val selectionKey: String,
    val title: String,
    val artist: String,
    val albumImageUrl: String? = null,
)
