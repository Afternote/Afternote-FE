package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
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
 * Screen을 직접 쓰지 않고 Entry로 감싸는 이유:
 * (1) 공용 [SongPlaylistScreen]은 여러 호출부(수신자 열람·작성자 관리·노래 추가)가 공유하므로
 *     이 화면 전용 지식([Song] 도메인 매핑, 타이틀, "총 N곡" 헤더·삭제 액션바 크롬)을 넣을 수 없고,
 * (2) 그 전용 지식을 NavGraph destination 블록에 인라인하면 ViewModel 없이 렌더할 수 없어
 *     Preview·스크린샷 테스트가 막힌다.
 * Entry가 둘 사이에서 도메인→표시 모델 매핑과 화면 전용 크롬 주입을 맡는 stateless 어댑터다.
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
        Text(
            text = stringResource(R.string.afternote_editor_playlist_song_count_format, songCount),
            style =
                AfternoteDesign.typography.bodySmallR.copy(
                    color = AfternoteDesign.colors.gray9,
                ),
        )
    }
}

/**
 * 선택 시 노출되는 하단 삭제 액션 바.
 * [AfternoteButton] Variant5 dual-action 으로 "전체 삭제 | 선택 삭제" 두 클릭 타깃을 그리고,
 * 리스트 위에 떠 있는 바라서 그림자만 이 래퍼에서 얹는다 (같은 화면의 추가하기 버튼과 동일한 5dp).
 */
@Composable
private fun MemorialPlaylistActionBar(
    onDeleteAllClick: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AfternoteButton(
        text = stringResource(R.string.afternote_editor_playlist_delete_all),
        onClick = onDeleteAllClick,
        type = AfternoteButtonType.Variant5,
        secondaryText = stringResource(R.string.afternote_editor_playlist_delete_selected),
        onSecondaryClick = onDeleteSelectedClick,
    )
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
