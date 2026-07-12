package com.afternote.feature.afternote.presentation.shared.detail.song

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

// region ── Screen-level config ──

/**
 * 검색 상태 바인딩 — 쿼리와 변경 콜백은 항상 한 몸이라 하나로 묶는다 (반쪽 제공이 타입상 불가능).
 *
 * @param query 현재 검색 텍스트
 * @param onQueryChange 검색 텍스트 변경 콜백
 */
data class SongSearchBinding(
    val query: String,
    val onQueryChange: (String) -> Unit,
)

// endregion

// region ── SongPlaylistScreen (full screen composables) ──

/**
 * 노래 검색 + 목록 전체 화면 (view-only).
 * Scaffold(HomeTopBar + BottomBar) + PlaylistSongList.
 *
 * @param title TopBar에 표시할 타이틀
 * @param onBackClick 뒤로가기 콜백
 * @param songs 표시할 노래 목록
 * @param defaultBottomNavTab 초기 선택 BottomNavTab
 */
@Composable
fun SongPlaylistScreen(
    modifier: Modifier = Modifier,
    title: String,
    onBackClick: () -> Unit,
    songs: List<PlaylistSongDisplay>,
    defaultBottomNavTab: BottomNavTab = BottomNavTab.NOTE,
) {
    var searchQuery by remember { mutableStateOf("") }
    // view-only 는 로컬 검색: 이미 담긴 목록을 클라 문자열로 좁힌다 (필터 소유권은 리스트가 아닌 여기).
    // remember(songs, searchQuery): 두 키가 이전 recomposition 과 == 로 같으면 캐시 반환, 하나라도
    // 바뀌면 재필터 — 무관한 recomposition 의 낭비 재계산 방지 (키=계산의 입력 전부).
    val visibleSongs = remember(songs, searchQuery) { filterSongsByQuery(songs, searchQuery) }

    SongPlaylistScaffold(
        title = title,
        onBackClick = onBackClick,
        modifier = modifier,
    ) { paddingValues ->
        PlaylistSongList(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            songs = visibleSongs,
            contentPadding = PaddingValues(horizontal = 20.dp),
            header = {
                SongSearchSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                )
            },
        )
    }
}

/**
 * 노래 검색 + 선택 가능 목록 전체 화면 (selectable).
 * 라디오 버튼 + 하단 추가하기 버튼 포함.
 *
 * @param title TopBar에 표시할 타이틀
 * @param onBackClick 뒤로가기 콜백
 * @param songs 표시할 노래 목록
 * @param onSongsSelected 추가하기 버튼 클릭 시 선택된 노래 목록 전달
 * @param search 검색어 소유·변경 바인딩 ([SongSearchBinding])
 * @param defaultBottomNavTab 초기 BottomNavTab
 * @param initialSelectedSongIds Preview·스크린샷 테스트용 초기 선택 (실화면은 빈 셋)
 */
@Composable
fun SelectableSongPlaylistScreen(
    modifier: Modifier = Modifier,
    title: String,
    onBackClick: () -> Unit,
    songs: List<PlaylistSongDisplay>,
    onSongsSelected: (List<PlaylistSongDisplay>) -> Unit,
    search: SongSearchBinding,
    defaultBottomNavTab: BottomNavTab = BottomNavTab.NOTE,
    initialSelectedSongIds: Set<String> = emptySet(),
) {
    SongPlaylistScaffold(
        title = title,
        onBackClick = onBackClick,
        modifier = modifier,
    ) { paddingValues ->
        SelectableSongListBody(
            modifier = Modifier.padding(paddingValues),
            songs = songs,
            header = {
                SongSearchSection(
                    searchQuery = search.query,
                    onSearchQueryChange = search.onQueryChange,
                )
            },
            contentPadding = PaddingValues(horizontal = 20.dp),
            initialSelectedSongIds = initialSelectedSongIds,
        ) { selectedIds, _ ->
            SongAddButton(
                count = selectedIds.size,
                onClick = { onSongsSelected(songs.filter { it.id in selectedIds }) },
            )
        }
    }
}

/**
 * 노래 선택 + 커스텀 하단 액션 바가 있는 플레이리스트 화면 (관리 모드).
 * header로 "총 N곡" + 노래 추가하기 등 헤더를 넣고,
 * selectionBottomBar로 선택 시 표시할 액션 바(예: 전체 삭제/선택 삭제)를 넣을 수 있음.
 *
 * @param title HomeTopBar 타이틀
 * @param onBackClick 뒤로가기 콜백
 * @param songs 표시할 노래 목록
 * @param header 헤더 composable (목록 첫 아이템으로 렌더)
 * @param selectionBottomBar 선택 시 하단 액션 바 (selectedIds, onClearSelection 제공)
 * @param defaultBottomNavTab 초기 BottomNavTab
 * @param initialSelectedSongIds Preview용 초기 선택 ID
 */
@Composable
fun ManageableSongPlaylistScreen(
    modifier: Modifier = Modifier,
    title: String,
    onBackClick: () -> Unit,
    songs: List<PlaylistSongDisplay>,
    header: @Composable () -> Unit,
    selectionBottomBar: @Composable (selectedIds: Set<String>, onClearSelection: () -> Unit) -> Unit,
    defaultBottomNavTab: BottomNavTab = BottomNavTab.NOTE,
    initialSelectedSongIds: Set<String> = emptySet(),
) {
    SongPlaylistScaffold(
        title = title,
        onBackClick = onBackClick,
        modifier = modifier,
    ) { paddingValues ->
        SelectableSongListBody(
            modifier = Modifier.padding(paddingValues),
            songs = songs,
            header = header,
            contentPadding = PaddingValues(horizontal = 20.dp),
            initialSelectedSongIds = initialSelectedSongIds,
            bottomBar = selectionBottomBar,
        )
    }
}

// endregion

// region ── Previews ──

@Preview(showBackground = true, name = "View-only 모드")
@Composable
private fun SongPlaylistScreenPreview() {
    val songs =
        (1..5).map { i ->
            PlaylistSongDisplay(id = "$i", title = "노래 제목 $i", artist = "가수 이름")
        }
    SongPlaylistScreen(
        title = "추억 플레이리스트",
        onBackClick = {},
        songs = songs,
        defaultBottomNavTab = BottomNavTab.NOTE,
    )
}

@Preview(showBackground = true, name = "선택 모드")
@Composable
private fun SongPlaylistScreenSelectablePreview() {
    val songs =
        (1..5).map { i ->
            PlaylistSongDisplay(id = "$i", title = "노래 제목 $i", artist = "가수 이름")
        }
    SelectableSongPlaylistScreen(
        title = "추억 플레이리스트 추가",
        onBackClick = {},
        songs = songs,
        onSongsSelected = {},
        search = SongSearchBinding(query = "아이유", onQueryChange = {}),
        initialSelectedSongIds = setOf("1", "3"),
    )
}

// endregion
