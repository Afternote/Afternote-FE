package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.modifierextention.FadingEdgeDirection
import com.afternote.core.ui.modifierextention.horizontalFadingEdge
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover

/**
 * 추억 플레이리스트 카드 (수신자 뷰·작성자 편집 공통).
 *
 * 시안 레이아웃(카드 1개 안): **헤더(🎵 라벨 + 우측 화살표) → 앨범 커버 가로 스크롤 → "현재 N개" 하단.**
 *
 * 화살표/카드 클릭 = 단일 진입 액션 [onCardClick]. 렌더는 모드와 무관하게 동일하므로 콜백을 하나로
 * 받는다 — 의미는 호출부가 결정한다 (수신자 뷰 = 전체 플레이리스트 보기, 작성자 편집 = 노래 추가).
 *
 * @param onCardClick 카드/화살표 클릭 액션. null 이면 화살표 숨김·클릭 비활성
 * @param albumItemContent 앨범 셀 커스텀; null이면 기본 회색 박스 (view/placeholder용)
 */
@Composable
fun MemorialPlaylist(
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.afternote_editor_playlist_screen_title),
    songCount: Int = 0,
    albumCovers: List<AlbumCover> = emptyList(),
    onCardClick: (() -> Unit)? = null,
    albumItemContent: (@Composable (album: AlbumCover, index: Int) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(size = 6.dp)
    val border = BorderStroke(width = 1.dp, color = AfternoteDesign.colors.gray2)
    // Surface 가 둥글기(clip)·배경·외곽선·(onClick 시) ripple 경계를 한 번에 옳게 처리 — 수동 modifier 순서 관리 불필요.
    // 클릭/비클릭이 별도 오버로드라 onCardClick null 여부로만 분기한다.
    if (onCardClick != null) {
        Surface(
            onClick = onCardClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = AfternoteDesign.colors.white,
            border = border,
        ) {
            MemorialPlaylistCardContent(label, songCount, albumCovers, onCardClick, albumItemContent)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = AfternoteDesign.colors.white,
            border = border,
        ) {
            MemorialPlaylistCardContent(label, songCount, albumCovers, onCardClick, albumItemContent)
        }
    }
}

@Composable
private fun MemorialPlaylistCardContent(
    label: String,
    songCount: Int,
    albumCovers: List<AlbumCover>,
    onCardClick: (() -> Unit)?,
    albumItemContent: (@Composable (album: AlbumCover, index: Int) -> Unit)?,
) {
    Column(modifier = Modifier.padding(all = 21.dp)) {
        // 헤더: 🎵 라벨 + (액션 있으면) 우측 화살표
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.afternote_ic_playlist_header),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AfternoteDesign.colors.gray6,
                )
                // 타이포 토큰엔 색이 없어 color 생략 시 LocalContentColor(컨테이너 의존, 여기선 순검정)로
                // 떨어진다 — 디자인 토큰 색은 항상 명시.
                Text(
                    text = label,
                    style =
                        AfternoteDesign.typography.primaryButton.copy(
                            color = AfternoteDesign.colors.gray9,
                        ),
                )
            }
            if (onCardClick != null) {
                // 피그마 실측 4x7 — 홈·mindrecord 등 전역 관용구(RightArrowIcon + size(4,7))와 동일
                RightArrowIcon(
                    modifier = Modifier.size(width = 4.dp, height = 7.dp),
                    tint = AfternoteDesign.colors.gray9,
                )
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        // 빈 목록이면 LazyRow 가 0 높이로 아무것도 그리지 않으므로 가드 없이 항상 렌더해도 결과 동일.
        MemorialPlaylistAlbumRow(
            albumCovers = albumCovers,
            albumItemContent = albumItemContent,
        )
        Spacer(modifier = Modifier.size(24.dp))
        Text(
            text = stringResource(R.string.afternote_detail_playlist_song_count, songCount),
            style =
                AfternoteDesign.typography.bodySmallR.copy(
                    color = AfternoteDesign.colors.black,
                ),
        )
    }
}

@Composable
private fun MemorialPlaylistAlbumRow(
    albumCovers: List<AlbumCover>,
    albumItemContent: (@Composable (album: AlbumCover, index: Int) -> Unit)?,
) {
    val listState = rememberLazyListState()
    val needsHorizontalFade by remember {
        derivedStateOf {
            listState.canScrollBackward || listState.canScrollForward
        }
    }
    val fadingDirection by remember {
        derivedStateOf {
            when {
                listState.canScrollBackward && listState.canScrollForward -> {
                    FadingEdgeDirection.BOTH
                }

                listState.canScrollBackward -> {
                    FadingEdgeDirection.LEFT
                }

                listState.canScrollForward -> {
                    FadingEdgeDirection.RIGHT
                }

                else -> {
                    FadingEdgeDirection.RIGHT
                }
            }
        }
    }
    LazyRow(
        state = listState,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (needsHorizontalFade) {
                        // edgeWidth = 그림자가 아니라 가장자리에서 커버가 투명하게 녹아드는(fade-out) 구간 너비.
                        Modifier.horizontalFadingEdge(
                            edgeWidth = 48.dp,
                            direction = fadingDirection,
                        )
                    } else {
                        Modifier
                    },
                ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(albumCovers) { index, album ->
            if (albumItemContent != null) {
                albumItemContent(album, index)
            } else {
                MemorialPlaylistAlbumCoverBox(album = album)
            }
        }
    }
}

@Composable
private fun MemorialPlaylistAlbumCoverBox(album: AlbumCover) {
    val modifier =
        Modifier
            .size(87.dp)
            .clip(RoundedCornerShape(8.dp))
    if (!album.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = album.imageUrl,
            contentDescription = stringResource(R.string.afternote_content_description_album_cover),
            modifier = modifier,
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.afternote_img_placeholder_1),
        )
    } else {
        // 둥글기는 공유 modifier 의 clip(8.dp) 이 이미 처리 — background 에 shape 중복 지정 불필요.
        Box(
            modifier = modifier.background(color = AfternoteDesign.colors.gray3),
        )
    }
}
