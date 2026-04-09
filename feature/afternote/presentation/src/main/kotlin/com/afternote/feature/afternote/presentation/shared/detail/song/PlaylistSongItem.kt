package com.afternote.feature.afternote.presentation.shared.detail.song

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.afternote.core.ui.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

private const val TAG = "PlaylistSongItem"

/**
 * 추모 플레이리스트·노래 추가 등에서 공통으로 쓰는 노래 한 줄 아이템.
 *
 * UI: 앨범 48dp(gray8 placeholder), 제목 Bold 14sp Gray9, 가수 12sp Gray6, 하단 Gray6 1dp 구분선.
 *
 * - [onClick]이 있으면 클릭 가능, [trailingContent]로 라디오 버튼 등 오른쪽 UI 삽입 (없으면 null)
 *
 * @param song 표시용 모델 [PlaylistSongDisplay] (Feature별 Song/Entity에서 매핑)
// * @param displayIndex 목록 내 순번 (이미지/placeholder용, 현재는 미사용, API 호환용)
 * @param onClick 클릭 시 호출 (null이면 비클릭)
 * @param trailingContent Row 오른쪽 콘텐츠 (AddSongScreen·MemorialPlaylistRouteScreen에서는 라디오)
 */
@Composable
fun PlaylistSongItem(
    song: PlaylistSongDisplay,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val base =
        if (onClick != null) {
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        } else {
            Modifier.fillMaxWidth()
        }

    Column(modifier = base) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .bottomBorder(color = AfternoteDesign.colors.gray3, width = 1.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumCoverBox(albumImageUrl = song.albumImageUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = song.title,
                        style =
                            AfternoteDesign.typography.bodySmallB.copy(
                                color = AfternoteDesign.colors.gray9,
                            ),
                    )
                    Text(
                        text = song.artist,
                        style =
                            AfternoteDesign.typography.captionLargeR.copy(
                                color = AfternoteDesign.colors.gray6,
                            ),
                    )
                }
                if (trailingContent != null) {
                    trailingContent()
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AlbumCoverBox(albumImageUrl: String?) {
    val modifier =
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(4.dp))
    if (!albumImageUrl.isNullOrBlank()) {
        val context = LocalContext.current
        val imageRequest =
            remember(albumImageUrl) {
                ImageRequest
                    .Builder(context)
                    .data(albumImageUrl)
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
                    "Coil load failed: albumImageUrl=$albumImageUrl",
                    state.result.throwable,
                )
            },
        )
    } else {
        Box(
            modifier = modifier.background(AfternoteDesign.colors.gray8),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistSongItemPreview() {
    PlaylistSongItem(
        song = PlaylistSongDisplay(id = "1", title = "노래 제목", artist = "가수 이름"),
    )
}
