package com.afternote.feature.afternote.presentation.shared.detail.song

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Slots for [SearchableSongList]: optional trailing (per row) and leading (header) content.
 *
 * @param trailingContent Optional composable for each row (e.g. radio button).
 * @param leadingContent Optional composable for the first item (e.g. custom header).
 */
data class SearchableSongListSlots(
    val trailingContent: (@Composable RowScope.(PlaylistSongDisplay) -> Unit)? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
)

// region ── SearchableSongList (list-level composable) ──

internal fun filterSongsByQuery(
    songs: List<PlaylistSongDisplay>,
    searchQuery: String,
): List<PlaylistSongDisplay> {
    val query = searchQuery.trim().lowercase()
    if (query.isEmpty()) return songs
    return songs.filter { song ->
        song.title.lowercase().contains(query) ||
            song.artist.lowercase().contains(query)
    }
}

@Composable
private fun SearchableSongListRow(
    song: PlaylistSongDisplay,
    onSongClick: ((PlaylistSongDisplay) -> Unit)?,
    trailingContent: (@Composable RowScope.(PlaylistSongDisplay) -> Unit)?,
) {
    PlaylistSongItem(
        song = song,
        onClick =
            if (onSongClick != null) {
                { onSongClick(song) }
            } else {
                null
            },
        trailingContent =
            if (trailingContent != null) {
                { trailingContent(song) }
            } else {
                null
            },
    )
}

/**
 * 검색창 + 노래 목록 패턴.
 * SongPlaylistScreen 내부에서 사용하거나, 커스텀 Scaffold가 필요한 경우 직접 사용.
 *
 * @param songs 표시할 노래 목록
 * @param searchQuery 현재 검색 텍스트
 * @param onSearchQueryChange 검색 텍스트 변경 콜백
 * @param onSongClick 노래 행 클릭 콜백 (null이면 비클릭)
 * @param contentPadding LazyColumn contentPadding — 파라미터 관통이 필수: 바깥 modifier.padding 은 스크롤
 *   콘텐츠가 경계에서 잘리지만 contentPadding 은 콘텐츠가 패딩 영역 밑을 지나가며 스크롤된다.
 *   값도 호출부 상태에 의존 (예: 선택 모드의 하단 플로팅 바 높이만큼 bottom 확보 — 바는 이 리스트 밖에 그려져 내부에선 알 수 없음)
 * @param slots Optional trailing (per row) and leading (header) content; nulls use defaults.
 */
@Composable
fun SearchableSongList(
    modifier: Modifier = Modifier,
    songs: List<PlaylistSongDisplay>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSongClick: ((PlaylistSongDisplay) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    slots: SearchableSongListSlots = SearchableSongListSlots(),
) {
    val focusManager = LocalFocusManager.current
    val filteredSongs =
        remember(songs, searchQuery) {
            filterSongsByQuery(songs, searchQuery)
        }
    LazyColumn(
        modifier =
            modifier
                .fillMaxWidth()
                .addFocusCleaner(focusManager),
        contentPadding = contentPadding,
    ) {
        item {
            // 헤더 = 슬롯 있으면 그 조각, 없으면 기본 검색 섹션
            if (slots.leadingContent != null) {
                slots.leadingContent.invoke()
            } else {
                SongSearchSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                )
            }
        }
        itemsIndexed(filteredSongs) { _, song ->
            SearchableSongListRow(
                song = song,
                onSongClick = onSongClick,
                trailingContent = slots.trailingContent,
            )
        }
    }
}

// endregion

// region ── Private sub-components ──

@Composable
private fun SongSearchSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
) {
    // vertical 20 은 이 조각의 고유 여백(위: 탑바와, 아래: 첫 곡과) — 바깥 contentPadding.top 은
    // "첫 아이템 아래" 간격을 못 만들고, 헤더 슬롯 교체(관리 모드 "총 N곡") 시 여백도 함께
    // 교체돼야 하므로 리스트가 아닌 조각이 소유한다.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.song_search_label),
            style =
                AfternoteDesign.typography.textField.copy(
                    fontWeight = FontWeight.Medium,
                    color = AfternoteDesign.colors.gray9,
                ),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        val searchFieldState = rememberTextFieldState(searchQuery)
        LaunchedEffect(searchQuery) {
            if (searchFieldState.text.toString() != searchQuery) {
                searchFieldState.edit { replace(0, length, searchQuery) }
            }
        }
        LaunchedEffect(Unit) {
            snapshotFlow { searchFieldState.text.toString() }
                .distinctUntilChanged()
                .collect { onSearchQueryChange(it) }
        }
        SongSearchTextField(
            state = searchFieldState,
            placeholder = stringResource(R.string.song_search_placeholder),
        )
    }
}

/**
 * 곡 검색용 텍스트 필드.
 *
 * 52dp 최소 높이·14dp 세로 패딩·검색 아이콘은 이 섹션 고유 사양이라
 * [BasicTextField]로 직접 구현합니다.
 */
@Composable
private fun SongSearchTextField(
    state: TextFieldState,
    placeholder: String,
) {
    val shape = RoundedCornerShape(8.dp)
    BasicTextField(
        state = state,
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .background(AfternoteDesign.colors.white, shape)
                .border(1.dp, AfternoteDesign.colors.gray2, shape),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        textStyle =
            AfternoteDesign.typography.bodySmallR.copy(
                color = AfternoteDesign.colors.gray9,
            ),
        cursorBrush = SolidColor(AfternoteDesign.colors.black),
        decorator = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (state.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style =
                                AfternoteDesign.typography.bodyBase.copy(
                                    lineHeight = 20.sp,
                                ),
                            color = AfternoteDesign.colors.gray4,
                        )
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.feature_afternote_ic_search),
                    contentDescription = stringResource(R.string.song_search_label),
                    tint = AfternoteDesign.colors.gray9,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    )
}

// endregion

// region ── Previews ──

@Preview(showBackground = true, name = "SearchableSongList 단독")
@Composable
private fun SearchableSongListPreview() {
    val songs =
        (1..5).map { i ->
            PlaylistSongDisplay(id = "$i", title = "노래 제목 $i", artist = "가수 이름")
        }
    var query by remember { mutableStateOf("") }
    SearchableSongList(
        songs = songs,
        searchQuery = query,
        onSearchQueryChange = { query = it },
        contentPadding = PaddingValues(horizontal = 20.dp),
    )
}

// endregion
