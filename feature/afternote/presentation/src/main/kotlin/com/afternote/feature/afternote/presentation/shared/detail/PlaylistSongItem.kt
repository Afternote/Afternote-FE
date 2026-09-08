package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * 추억 플레이리스트·노래 추가 등에서 공통으로 쓰는 노래 한 줄 아이템.
 *
 * UI: 앨범 48dp(gray8 placeholder), 제목 Bold 14sp Gray9, 가수 12sp Gray6, 하단 Gray6 1dp 구분선.
 *
 * - [onClick]이 있으면 클릭 가능, [selected]로 복수 선택 체크 표시 (null이면 선택 UI 없음)
 * - [onClick] 콜백은 [song] 을 실어 되돌려준다: 행이 이미 자기 곡을 알므로, 호출부(리스트 루프)가
 *   곡 캡처용 래핑 람다를 만들지 않고 per-song 람다를 그대로 넘길 수 있다.
 *
 * @param song 표시용 모델 [PlaylistSongDisplay] (Feature별 Song/Entity에서 매핑)
// * @param displayIndex 목록 내 순번 (이미지/placeholder용, 현재는 미사용, API 호환용)
 * @param onClick 클릭 시 이 행의 [song] 과 함께 호출 (null이면 비클릭)
 * @param selected 복수 선택 상태 (true/false=체크박스 표시, null=선택 semantics 없음 — 예: view-only 열람)
 */
@Composable
fun PlaylistSongItem(
    song: PlaylistSongDisplay,
    onClick: ((PlaylistSongDisplay) -> Unit)? = null,
    selected: Boolean? = null,
) {
    val fullWidthModifier = Modifier.fillMaxWidth()
    val interactionModifier =
        when {
            onClick == null -> {
                fullWidthModifier
            }

            selected != null -> {
                fullWidthModifier.toggleable(
                    value = selected,
                    role = Role.Checkbox,
                    onValueChange = { onClick(song) },
                )
            }

            else -> {
                fullWidthModifier.clickable { onClick(song) }
            }
        }
    val rowModifier =
        interactionModifier
            .bottomBorder(color = AfternoteDesign.colors.gray3, width = 1.dp)
            .padding(8.dp)

    Row(
        modifier = rowModifier,
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
                    AfternoteDesign.typography.bodySmallR.copy(
                        color = AfternoteDesign.colors.gray9,
                    ),
            )
            Text(
                text = song.artist,
                style =
                    AfternoteDesign.typography.bodySmallR.copy(
                        color = AfternoteDesign.colors.gray9,
                    ),
            )
        }
        selected?.let { SongSelectionCheckbox(selected = it) }
    }
}

/**
 * 선택/관리 모드 행 우측의 원형 체크박스 (24dp·core 선택 색상).
 * PlaylistSongItem 의 selected 가 non-null 일 때만 렌더된다.
 */
@Composable
private fun SongSelectionCheckbox(selected: Boolean) {
    AfternoteCircularCheckbox(
        state = if (selected) CheckboxState.Default else CheckboxState.None,
        size = 24.dp,
        modifier = Modifier.clearAndSetSemantics {},
    )
}

@Composable
private fun AlbumCoverBox(albumImageUrl: String?) {
    val modifier =
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(2.dp))
    if (albumImageUrl.isNullOrBlank()) {
        Box(
            modifier = modifier.background(AfternoteDesign.colors.gray8),
        )
    } else {
        AsyncImage(
            model = albumImageUrl,
            contentDescription = stringResource(R.string.afternote_content_description_album_cover),
            modifier = modifier,
            contentScale = ContentScale.Crop,
            // 로드 실패 폴백 = URL 없음과 동일한 gray8 (더미 placeholder 이미지 대신 통일).
            error = ColorPainter(AfternoteDesign.colors.gray8),
        )
    }
}
