package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteLightTheme
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreen
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreenSelectableOptions
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * 노래 추가하기 화면 (API 검색 연동).
 *
 * ViewModel 의존성 없이 순수하게 UI만 그립니다.
 */
@Composable
fun AddSongScreen(
    uiState: AddSongUiState,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSongsAdded: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    SongPlaylistScreen(
        modifier = modifier,
        title = stringResource(R.string.afternote_editor_playlist_add_screen_title),
        onBackClick = onBackClick,
        songs = uiState.songs,
        onSongsSelected = { selected ->
            onSongsAdded(selected.map(::toSong))
        },
        options =
            SongPlaylistScreenSelectableOptions(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
            ),
    )
}

/**
 * 노래 추가하기 화면 (Preview·더미용).
 * 정적 목록만 표시하며 검색은 클라이언트 필터만 적용.
 *
 * @param songs 표시할 노래 목록
 * @param initialSelectedSongIds Preview용. 넣으면 해당 ID가 선택된 상태로 시작
 */
@Composable
fun AddSongScreen(
    songs: List<PlaylistSongDisplay>,
    onBackClick: () -> Unit,
    onSongsAdded: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedSongIds: Set<String>? = null,
) {
    SongPlaylistScreen(
        modifier = modifier,
        title = stringResource(R.string.afternote_editor_playlist_add_screen_title),
        onBackClick = onBackClick,
        songs = songs,
        onSongsSelected = { selected ->
            onSongsAdded(selected.map(::toSong))
        },
        options = SongPlaylistScreenSelectableOptions(initialSelectedSongIds = initialSelectedSongIds),
    )
}

private fun toSong(display: PlaylistSongDisplay): Song =
    Song(
        id = display.id,
        title = display.title,
        artist = display.artist,
        albumCoverUrl = display.albumImageUrl,
    )

@Preview(showBackground = true)
@Composable
private fun AddSongScreenPreview() {
    AfternoteLightTheme {
        AddSongScreen(
            songs =
                (1..9).map { i ->
                    PlaylistSongDisplay(id = "s$i", title = "노래 제목 $i", artist = "가수 이름")
                },
            onBackClick = {},
            onSongsAdded = {},
        )
    }
}

@Preview(showBackground = true, name = "추가하기 버튼 노출")
@Composable
private fun AddSongScreenAddButtonPreview() {
    AfternoteLightTheme {
        AddSongScreen(
            songs =
                (1..9).map { i ->
                    PlaylistSongDisplay(id = "s$i", title = "노래 제목 $i", artist = "가수 이름")
                },
            onBackClick = {},
            onSongsAdded = {},
            initialSelectedSongIds = setOf("s1", "s3"),
        )
    }
}

@Preview(showBackground = true, name = "API 검색 연동")
@Composable
private fun AddSongScreenWithSearchPreview() {
    AfternoteLightTheme {
        AddSongScreen(
            uiState =
                AddSongUiState(
                    songs =
                        (1..5).map { i ->
                            PlaylistSongDisplay(
                                id = "f$i",
                                title = "노래 $i",
                                artist = "가수",
                            )
                        },
                    searchQuery = "아이유",
                ),
            onSearchQueryChange = {},
            onBackClick = {},
            onSongsAdded = {},
        )
    }
}
