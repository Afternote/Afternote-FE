package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteLightTheme
import com.afternote.feature.afternote.presentation.shared.detail.song.SelectableSongListBody
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScaffold
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * 추억 플레이리스트 Entry.
 *
 * graph-scoped [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] SSOT의 곡 목록을
 * 공용 부품([SongPlaylistScaffold] + [SelectableSongListBody])의 입력 형태로 매핑한다. 변경은 콜백 인텐트로 위임한다.
 *
 * 이 조립을 NavGraph destination 에 인라인하지 않고 Entry로 빼는 이유:
 * (1) 공용 SongPlaylist 화면 계열(shared/detail/song)은 여러 화면이 공유하는 범용 부품이라
 *     이 화면 전용 지식([Song] 도메인 매핑, 타이틀, "총 N곡" 헤더·삭제 라벨·콜백)을 넣을 수 없고,
 * (2) 그 전용 지식을 NavGraph destination 블록에 인라인하면 ViewModel 없이 렌더할 수 없어
 *     Preview·스크린샷 테스트가 막힌다.
 * Entry가 둘 사이에서 도메인→표시 모델 매핑과 화면 전용 크롬 주입을 맡는 stateless 어댑터다.
 *
 * @param songs graph-scoped HostViewModel에서 collect한 현재 곡 목록 스냅샷
 * @param onBackClick 뒤로가기
 * @param onNavigateToAddSongScreen 노래 추가 화면 진입 (현재 버튼 미노출·배선만 유지)
 * @param onClearAllSongs 전체 삭제
 * @param onRemoveSongs 선택 삭제
 * @param initialSelectedSongIds Preview용. 넣으면 해당 ID가 선택된 상태로 시작 (기본 빈 셋)
 */
@Composable
fun MemorialPlaylistEntry(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigateToAddSongScreen: () -> Unit = {},
    onClearAllSongs: () -> Unit = {},
    onRemoveSongs: (Set<String>) -> Unit = {},
    initialSelectedSongIds: Set<String> = emptySet(),
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
    SongPlaylistScaffold(
        title = stringResource(R.string.afternote_editor_playlist_screen_title),
        onBackClick = onBackClick,
        modifier = modifier,
    ) { paddingValues ->
        SelectableSongListBody(
            modifier = Modifier.padding(paddingValues),
            songs = displaySongs,
            header = {
                MemorialPlaylistListHeader(songCount = displaySongs.size)
            },
            initialSelectedSongIds = initialSelectedSongIds,
            actionLabel = stringResource(R.string.afternote_editor_playlist_delete_all),
            onAction = { onClearAllSongs() },
            secondaryActionLabel = stringResource(R.string.afternote_editor_playlist_delete_selected),
            onSecondaryAction = onRemoveSongs,
        )
    }
}

/**
 * MemorialPlaylistList 화면 상단 헤더: 선택 모드와 무관하게 항상 "총 N곡"만 왼쪽에 표시.
 *
 * "노래 추가하기" 진입 버튼은 시안에 없어 제거 — 부재가 의도인지 디자이너 확인 대기,
 * 답변에 따라 복원 여부 결정 (onNavigateToAddSongScreen 배선은 유지).
 */
@Composable
private fun MemorialPlaylistListHeader(
    songCount: Int,
    modifier: Modifier = Modifier,
) {
    // 헤더 상단 간격은 헤더별로 달라(검색 16 / 곡 수 8) 리스트가 아닌 각 헤더가 top 여백을 소유한다.
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.afternote_editor_playlist_song_count_format, songCount),
            style =
                AfternoteDesign.typography.bodySmallR.copy(
                    color = AfternoteDesign.colors.gray9,
                ),
        )
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
        )
    }
}

@Preview(showBackground = true, name = "선택 모드")
@Composable
private fun MemorialPlaylistEntrySelectionModePreview() {
    AfternoteLightTheme {
        MemorialPlaylistEntry(
            songs = memorialPlaylistPreviewSongs().take(4),
            initialSelectedSongIds = setOf("1", "3"),
        )
    }
}
