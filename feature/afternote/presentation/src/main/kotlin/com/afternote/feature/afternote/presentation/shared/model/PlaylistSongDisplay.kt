package com.afternote.feature.afternote.presentation.shared.model

import androidx.compose.runtime.Immutable

/**
 * 추억 플레이리스트·노래 추가 등에서 재사용하는 공통 표시용 모델.
 *
 * Feature별 Song/Entity는 각자 [PlaylistSongDisplay]로 매핑하여 사용합니다.
 *
 * @param selectionKey 클릭·선택 시 화면 안에서 곡을 구분하는 키
 * @param title 노래 제목
 * @param artist 가수명
 * @param albumImageUrl 앨범 이미지 URL (선택, 음악 검색 API 등에서 사용)
 */
@Immutable
data class PlaylistSongDisplay(
    val selectionKey: String,
    val title: String,
    val artist: String,
    val albumImageUrl: String? = null,
)
