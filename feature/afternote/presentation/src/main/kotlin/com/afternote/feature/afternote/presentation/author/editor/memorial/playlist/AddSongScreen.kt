package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteLightTheme
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreen
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreenSelectableOptions
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * AddSong 화면의 외부 결과 콜백 묶음.
 *
 * Entry → Screen 으로 콜백을 묶어 전달해 시그니처 폭주를 막는다.
 * Screen 내부 입력 신호 ([onSearchQueryChange]·[onErrorConsumed]) 와는 분리 — 그쪽은 VM verb 메서드 직접 노출.
 */
@Immutable
data class AddSongCallbacks(
    val onBackClick: () -> Unit,
    val onSongsAdded: (List<Song>) -> Unit,
)

/**
 * 노래 추가하기 화면 (API 검색 연동).
 *
 * ViewModel 의존성 없이 순수하게 UI만 그립니다. [AddSongUiState.error] 는 sealed [AddSongError]
 * 에서 UI 레이어가 [stringResource] 로 해석한 뒤 Snackbar 표출 → [onErrorConsumed] 로 VM 에 nullify
 * 신호. VM 이 Android Framework (Context/Resources) 를 의존하지 않도록 string resolve 는 본
 * 레이어에서만 수행 (#267). Snackbar 채택은 repo convention (Toast 2건 vs Snackbar 19건) + `showSnackbar`
 * suspend 큐 의미로 같은 [AddSongError.SearchFailedGeneric] (data object) 가 연속 발화해도 표출 누락 회피.
 */
@Composable
fun AddSongScreen(
    uiState: AddSongUiState,
    onSearchQueryChange: (String) -> Unit,
    onErrorConsumed: () -> Unit,
    callbacks: AddSongCallbacks,
    modifier: Modifier = Modifier,
) {
    val genericErrorMessage = stringResource(R.string.afternote_editor_search_failed_generic)
    val currentOnErrorConsumed by rememberUpdatedState(onErrorConsumed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        val message =
            when (error) {
                AddSongError.SearchFailedGeneric -> genericErrorMessage
                is AddSongError.SearchFailedWithMessage -> error.message
            }
        snackbarHostState.showSnackbar(message = message, withDismissAction = true)
        currentOnErrorConsumed()
    }

    Box(modifier = modifier.fillMaxSize()) {
        SongPlaylistScreen(
            title = stringResource(R.string.afternote_editor_playlist_add_screen_title),
            onBackClick = callbacks.onBackClick,
            songs = uiState.songs,
            onSongsSelected = { selected ->
                callbacks.onSongsAdded(selected.map(::toSong))
            },
            options =
                SongPlaylistScreenSelectableOptions(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                ),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
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
            onErrorConsumed = {},
            callbacks = AddSongCallbacks(onBackClick = {}, onSongsAdded = {}),
        )
    }
}
