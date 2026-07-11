package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteLightTheme
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreen
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreenManagementContent
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

data class MemorialPlaylistEntryActions(
    val onBackClick: () -> Unit = {},
    val onNavigateToAddSongScreen: () -> Unit = {},
    val onClearAllSongs: () -> Unit = {},
    val onRemoveSongs: (Set<String>) -> Unit = {},
)

/**
 * 추억 플레이리스트 Entry.
 *
 * graph-scoped [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] SSOT의 곡 목록을
 * 공용 [SongPlaylistScreen]의 입력 형태로 매핑한다. 변경은 [actions] 인텐트로 위임한다.
 *
 * @param songs graph-scoped HostViewModel에서 collect한 현재 곡 목록 스냅샷
 * @param actions 네비게이션 + 삭제 인텐트 콜백 모음
 * @param initialSelectedSongIds Preview용. 넣으면 해당 ID가 선택된 상태로 시작 (기본 null)
 */
@Composable
fun MemorialPlaylistEntry(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    actions: MemorialPlaylistEntryActions = MemorialPlaylistEntryActions(),
    initialSelectedSongIds: Set<String>? = null,
) {
    val displaySongs =
        songs.map { s ->
            PlaylistSongDisplay(
                id = s.id,
                title = s.title,
                artist = s.artist,
                albumImageUrl = s.albumCoverUrl,
            )
        }
    SongPlaylistScreen(
        modifier = modifier,
        title = stringResource(R.string.afternote_editor_playlist_screen_title),
        onBackClick = actions.onBackClick,
        songs = displaySongs,
        managementContent =
            SongPlaylistScreenManagementContent(
                leadingContent = {
                    MemorialPlaylistListHeader(songCount = displaySongs.size)
                },
                selectionBottomBar = { selectedIds, onClearSelection ->
                    MemorialPlaylistActionBar(
                        onDeleteAllClick = {
                            actions.onClearAllSongs()
                            onClearSelection()
                        },
                        onDeleteSelectedClick = {
                            actions.onRemoveSongs(selectedIds)
                            onClearSelection()
                        },
                    )
                },
            ),
        defaultBottomNavTab = BottomNavTab.NOTE,
        initialSelectedSongIds = initialSelectedSongIds,
    )
}

/**
 * MemorialPlaylistList 화면 상단 헤더: 선택 모드와 무관하게 항상 "총 N곡"만 왼쪽에 표시.
 *
 * "노래 추가하기" 진입 버튼은 시안에 없어 제거 — 부재가 의도인지 디자이너 확인 대기,
 * 답변에 따라 복원 여부 결정 ([MemorialPlaylistEntryActions.onNavigateToAddSongScreen] 배선은 유지).
 */
@Composable
private fun MemorialPlaylistListHeader(
    songCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
//        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.afternote_editor_playlist_song_count_format, songCount),
            style =
                AfternoteDesign.typography.bodySmallR.copy(
                    color = AfternoteDesign.colors.gray9,
                ),
        )
//        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MemorialPlaylistActionBar(
    onDeleteAllClick: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionBarShape = RoundedCornerShape(8.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 5.dp,
                    shape = actionBarShape,
                    clip = false,
                    ambientColor = AfternoteDesign.colors.black.copy(alpha = 38f / 255f),
                    spotColor = AfternoteDesign.colors.black.copy(alpha = 38f / 255f),
                ).background(color = AfternoteDesign.colors.white, shape = actionBarShape)
                .clip(actionBarShape),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(onClick = onDeleteAllClick)
                    .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.afternote_editor_playlist_delete_all),
                style =
                    AfternoteDesign.typography.textField.copy(
                        color = AfternoteDesign.colors.gray9,
                        textAlign = TextAlign.Center,
                    ),
            )
        }
        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(AfternoteDesign.colors.gray3),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(onClick = onDeleteSelectedClick)
                    .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.afternote_editor_playlist_delete_selected),
                style =
                    AfternoteDesign.typography.textField.copy(
                        color = AfternoteDesign.colors.gray9,
                        textAlign = TextAlign.Center,
                    ),
            )
        }
    }
}

private fun memorialPlaylistPreviewSongs(): List<Song> =
    (1..11).map { index ->
        Song(
            id = index.toString(),
            title = "노래 제목",
            artist = "가수 이름",
        )
    }

@Preview(showBackground = true)
@Composable
private fun MemorialPlaylistEntryPreview() {
    AfternoteLightTheme {
        MemorialPlaylistEntry(
            songs = memorialPlaylistPreviewSongs().take(3),
            actions = MemorialPlaylistEntryActions(),
        )
    }
}

@Preview(showBackground = true, name = "선택 모드")
@Composable
private fun MemorialPlaylistEntrySelectionModePreview() {
    AfternoteLightTheme {
        MemorialPlaylistEntry(
            songs = memorialPlaylistPreviewSongs().take(4),
            actions = MemorialPlaylistEntryActions(),
            initialSelectedSongIds = setOf("1", "3"),
        )
    }
}
