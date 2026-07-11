package com.afternote.feature.afternote.presentation.shared.detail.song

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.button.CustomRadioButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

// region ── Screen-level config ──

/**
 * Content slots for the management-mode [SongPlaylistScreen]: leading header and selection bottom bar.
 *
 * 공용 화면이 선택 상태(selectedIds)의 소유자이고, 그 위에 그릴 크롬은 호출부마다 달라
 * (수신자 열람 = 없음 / 작성자 관리 = 곡 수 헤더 + 삭제 액션바) 슬롯으로 주입받는다 —
 * Scaffold 의 topBar/bottomBar 와 같은 패턴. 선택 상태가 필요한 조각(selectionBottomBar)에만
 * 람다 인자로 내려주고, 상태 조작(선택 해제)은 onClearSelection 콜백으로 화면에 되돌린다.
 *
 * @param leadingContent Header composable (목록 첫 아이템으로 렌더 — 목록과 함께 스크롤).
 * @param selectionBottomBar Bottom bar composable when selection is non-empty.
 *   onClearSelection = 선택 **전체** 해제(emptySet — 바 노출 조건이 isNotEmpty 라 바 자체도 닫힘).
 *   선택 상태가 화면 내부 remember 라 호출부가 직접 못 만지므로, 소유자가 변이 함수를 내려주고
 *   호출부가 시점(액션 완료 직후)을 정해 호출한다.
 */
data class SongPlaylistScreenManagementContent(
    val leadingContent: @Composable () -> Unit,
    val selectionBottomBar: @Composable (selectedIds: Set<String>, onClearSelection: () -> Unit) -> Unit,
)

/**
 * Optional parameters for the selectable [SongPlaylistScreen] (S107: keep param count ≤7).
 */
data class SongPlaylistScreenSelectableOptions(
    val defaultBottomNavTab: BottomNavTab = BottomNavTab.NOTE,
    val initialSelectedSongIds: Set<String>? = null,
    val searchQuery: String? = null,
    val onSearchQueryChange: ((String) -> Unit)? = null,
)

// endregion

// region ── SongPlaylistScreen (full screen composable) ──

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
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = title,
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
    ) { paddingValues ->
        PlaylistSongList(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            songs = songs,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            contentPadding = PaddingValues(horizontal = 20.dp),
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
 * @param options [SongPlaylistScreenSelectableOptions] — 초기 BottomNavTab·초기 선택 ID(Preview 용)·검색 바인딩 묶음
 */
@Composable
fun SongPlaylistScreen(
    modifier: Modifier = Modifier,
    title: String,
    onBackClick: () -> Unit,
    songs: List<PlaylistSongDisplay>,
    onSongsSelected: (List<PlaylistSongDisplay>) -> Unit,
    options: SongPlaylistScreenSelectableOptions = SongPlaylistScreenSelectableOptions(),
) {
    val focusManager = LocalFocusManager.current
    var selectedSongIds by remember {
        mutableStateOf(
            options.initialSelectedSongIds ?: emptySet(),
        )
    }
    var internalSearchQuery by remember { mutableStateOf("") }

    val effectiveQuery = options.searchQuery ?: internalSearchQuery
    val effectiveOnSearchQueryChange = options.onSearchQueryChange ?: { internalSearchQuery = it }
    val displaySongs =
        if (options.searchQuery != null && options.onSearchQueryChange != null) {
            songs
        } else {
            filterSongsByQuery(songs, effectiveQuery)
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = title,
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            PlaylistSongList(
                modifier = Modifier.fillMaxSize(),
                songs = displaySongs,
                searchQuery = effectiveQuery,
                onSearchQueryChange = effectiveOnSearchQueryChange,
                onSongClick = { song ->
                    selectedSongIds =
                        if (song.id in selectedSongIds) {
                            selectedSongIds - song.id
                        } else {
                            selectedSongIds + song.id
                        }
                },
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                    ),
                slots =
                    PlaylistSongListSlots(
                        trailingContent = { song ->
                            SongSelectionRadio(selected = song.id in selectedSongIds)
                        },
                    ),
            )
            if (selectedSongIds.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                ) {
                    SongAddButton(
                        count = selectedSongIds.size,
                        onClick = {
                            val selected = displaySongs.filter { it.id in selectedSongIds }
                            onSongsSelected(selected)
                        },
                    )
                }
            }
        }
    }
}

/**
 * 노래 선택 + 커스텀 하단 액션 바가 있는 플레이리스트 화면 (관리 모드).
 * managementContent.leadingContent로 "총 N곡" + 노래 추가하기 등 헤더를 넣고,
 * managementContent.selectionBottomBar로 선택 시 표시할 액션 바(예: 전체 삭제/선택 삭제)를 넣을 수 있음.
 *
 * @param title HomeTopBar 타이틀
 * @param onBackClick 뒤로가기 콜백
 * @param songs 표시할 노래 목록
 * @param managementContent leadingContent + selectionBottomBar (헤더 및 하단 액션 바)
 * @param defaultBottomNavTab 초기 BottomNavTab
 * @param initialSelectedSongIds Preview용 초기 선택 ID
 */
@Composable
fun SongPlaylistScreen(
    modifier: Modifier = Modifier,
    title: String,
    onBackClick: () -> Unit,
    songs: List<PlaylistSongDisplay>,
    managementContent: SongPlaylistScreenManagementContent,
    defaultBottomNavTab: BottomNavTab = BottomNavTab.NOTE,
    initialSelectedSongIds: Set<String>? = null,
) {
    val focusManager = LocalFocusManager.current
    var selectedSongIds by remember {
        mutableStateOf(initialSelectedSongIds ?: emptySet())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = title,
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues),
        ) {
            PlaylistSongList(
                modifier = Modifier.fillMaxSize(),
                songs = songs,
                searchQuery = "",
                onSearchQueryChange = {},
                onSongClick = { song ->
                    selectedSongIds =
                        if (song.id in selectedSongIds) {
                            selectedSongIds - song.id
                        } else {
                            selectedSongIds + song.id
                        }
                },
                contentPadding =
                    PaddingValues(
                        top = 8.dp,
                        start = 20.dp,
                        end = 20.dp,
                    ),
                slots =
                    PlaylistSongListSlots(
                        trailingContent = { song ->
                            SongSelectionRadio(selected = song.id in selectedSongIds)
                        },
                        leadingContent = managementContent.leadingContent,
                    ),
            )
            if (selectedSongIds.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                ) {
                    managementContent.selectionBottomBar(selectedSongIds) {
                        selectedSongIds = emptySet()
                    }
                }
            }
        }
    }
}

// endregion

// region ── Screen-private sub-components ──

/**
 * 선택/관리 모드 행 우측의 선택 라디오 (24dp·gray9/gray4).
 *
 * 행의 trailingContent 슬롯은 내용물을 모르는 위치 계약(M3 ListItem 의 trailingContent 와 같은 관례)이라,
 * "라디오"라는 이름·스펙은 슬롯이 아니라 주입 조각인 여기에 붙는다. 선택·관리 두 오버로드가 공유.
 */
@Composable
private fun SongSelectionRadio(selected: Boolean) {
    CustomRadioButton(
        selected = selected,
        onClick = null,
        buttonSize = 24.dp,
        selectedColor = AfternoteDesign.colors.gray9,
        unselectedColor = AfternoteDesign.colors.gray4,
    )
}

/**
 * 선택 시에만 노출되는 추가하기 버튼.
 * - 연한 회색 배경, 왼쪽에 파란 원형 뱃지(선택 개수), 오른쪽 "추가하기"
 */
@Composable
private fun SongAddButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = 5.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = AfternoteDesign.colors.black.copy(alpha = 38f / 255f),
                    spotColor = AfternoteDesign.colors.black.copy(alpha = 38f / 255f),
                ).background(color = AfternoteDesign.colors.gray1, shape = shape)
                .clip(shape)
                .clickable(
                    onClick = {
                        focusManager.clearFocus()
                        onClick()
                    },
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Row {
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .background(
                            color = AfternoteDesign.colors.gray9,
                            shape = RoundedCornerShape(40.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$count",
                    style =
                        AfternoteDesign.typography.captionLargeR.copy(
                            fontWeight = FontWeight.Medium,
                            color = AfternoteDesign.colors.white,
                        ),
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.add_button),
                style =
                    AfternoteDesign.typography.textField.copy(
                        fontWeight = FontWeight.Medium,
                        color = AfternoteDesign.colors.gray9,
                    ),
            )
        }
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
    SongPlaylistScreen(
        title = "추억 플레이리스트 추가",
        onBackClick = {},
        songs = songs,
        onSongsSelected = {},
        options = SongPlaylistScreenSelectableOptions(initialSelectedSongIds = setOf("1", "3")),
    )
}

// endregion
