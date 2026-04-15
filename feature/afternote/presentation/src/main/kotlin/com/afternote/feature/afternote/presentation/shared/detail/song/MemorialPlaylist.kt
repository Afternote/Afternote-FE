package com.afternote.feature.afternote.presentation.shared.detail.song

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.afternote.core.model.AlbumCover
import com.afternote.core.ui.icon.ArrowIcon
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.modifierextention.FadingEdgeDirection
import com.afternote.core.ui.modifierextention.horizontalFadingEdge
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

private const val TAG = "MemorialPlaylist"

@Composable
private fun MemorialPlaylistSongCountRow(
    songCount: Int,
    showArrow: Boolean,
) {
    if (showArrow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "현재 ${songCount}개의 노래가 담겨 있습니다.",
                style =
                    AfternoteDesign.typography.bodySmallR.copy(
                        color = AfternoteDesign.colors.black,
                    ),
            )
            RightArrowIcon(
                modifier = Modifier.size(16.dp),
                tint = AfternoteDesign.colors.gray9,
            )
        }
    } else {
        Text(
            text = "현재 ${songCount}개의 노래가 담겨 있습니다.",
            style =
                AfternoteDesign.typography.bodySmallR.copy(
                    color = AfternoteDesign.colors.black,
                ),
        )
    }
}

@Composable
private fun MemorialPlaylistAddButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .background(color = AfternoteDesign.colors.gray9, shape = RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "노래 추가하기",
            style =
                AfternoteDesign.typography.captionLargeR.copy(
                    fontWeight = FontWeight.Medium,
                    color = AfternoteDesign.colors.gray9,
                ),
        )
        ArrowIcon(
            iconRes = R.drawable.feature_afternote_ic_arrow_right_playlist,
            contentDescription = "추가",
            modifier = Modifier.size(12.dp),
            tint = AfternoteDesign.colors.white,
        )
    }
}

/**
 * 추모 플레이리스트 컴포넌트 (edit·view 공통, 오버로딩으로 모드 구분).
 *
 * - **Edit mode**: onAddSongClick를 넘기면 "노래 추가하기" 버튼이 보이고, 카드 상단에는 개수 텍스트만 표시.
 * - **View mode**: null이면 버튼 없음, 개수 텍스트 오른쪽에 화살표 아이콘 표시.
 *
 * 앨범 행: albumCovers를 사용. albumItemContent를 넘기면 해당 슬롯으로 그리며, null이면 각 앨범을 회색 박스로 표시.
 *
 * @param onAddSongClick null이면 view 모드(버튼·편집 UI 없음), non-null이면 edit 모드
 * @param onPlaylistClick view 모드에서 카드(오른쪽 화살표 영역 포함) 클릭 시 호출. null이면 클릭 비활성화
 * @param albumItemContent 앨범 셀 커스텀; null이면 기본 회색 박스 (view/placeholder용)
 */
@Composable
fun MemorialPlaylist(
    modifier: Modifier = Modifier,
    label: String = "추모 플레이리스트",
    songCount: Int = 0,
    albumCovers: List<AlbumCover> = emptyList(),
    onAddSongClick: (() -> Unit)? = null,
    onPlaylistClick: (() -> Unit)? = null,
    albumItemContent: (@Composable (album: AlbumCover, index: Int) -> Unit)? = null,
) {
    val isEditMode = onAddSongClick != null
    val cardModifier =
        when {
            !isEditMode && onPlaylistClick != null -> {
                modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPlaylistClick)
            }

            else -> {
                modifier.fillMaxWidth()
            }
        }
    Column(
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = label,
            style =
                AfternoteDesign.typography.textField.copy(
                    fontWeight = FontWeight.Medium,
                    color = AfternoteDesign.colors.gray9,
                ),
        )
        Column(
            modifier =
                cardModifier
                    .background(color = AfternoteDesign.colors.white, shape = RoundedCornerShape(size = 16.dp))
                    .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            MemorialPlaylistSongCountRow(
                songCount = songCount,
                showArrow = !isEditMode,
            )
            if (albumCovers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                MemorialPlaylistAlbumRow(
                    albumCovers = albumCovers,
                    albumItemContent = albumItemContent,
                )
            }
            if (onAddSongClick != null) {
                MemorialPlaylistAddButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onAddSongClick,
                )
            }
        }
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
                        Modifier.horizontalFadingEdge(
                            edgeWidth = 45.dp,
                            direction = fadingDirection,
                        )
                    } else {
                        Modifier
                    },
                ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
    if (!album.imageUrl.isNullOrBlank()) {
        val context = LocalContext.current
        val imageRequest =
            remember(album.imageUrl) {
                ImageRequest
                    .Builder(context)
                    .data(album.imageUrl)
                    .httpHeaders(
                        NetworkHeaders
                            .Builder()
                            .apply {
                                this["User-Agent"] = "Afternote Android App"
                            }.build(),
                    ).build()
            }
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.content_description_album_cover),
            modifier = modifier,
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.feature_afternote_img_placeholder_1),
            onError = { state: AsyncImagePainter.State.Error ->
                Log.e(
                    TAG,
                    "Coil load failed: album.imageUrl=${album.imageUrl}",
                    state.result.throwable,
                )
            },
        )
    } else {
        Box(
            modifier =
                modifier.background(
                    color = AfternoteDesign.colors.gray3,
                    shape = RoundedCornerShape(8.dp),
                ),
        )
    }
}

@Preview(showBackground = true, name = "Edit mode")
@Composable
private fun MemorialPlaylistEditPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MemorialPlaylist(
                songCount = 4,
                albumCovers = PlaylistAlbumRowPreviewFixtures.albumCovers,
                onAddSongClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "View mode")
@Composable
private fun MemorialPlaylistViewPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MemorialPlaylist(
                songCount = 16,
                albumCovers = PlaylistAlbumRowPreviewFixtures.albumCovers,
                onAddSongClick = null,
            )
        }
    }
}
