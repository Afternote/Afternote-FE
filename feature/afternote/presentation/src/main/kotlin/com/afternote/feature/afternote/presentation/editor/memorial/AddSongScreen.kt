package com.afternote.feature.afternote.presentation.editor.memorial
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.SelectableSongListBody
import com.afternote.feature.afternote.presentation.shared.detail.SongPlaylistScaffold
import com.afternote.feature.afternote.presentation.shared.detail.SongSearchSection
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/** 검색 결과를 그린다. 상태 수집과 실패 신호 소비는 [AddSongEntry]가 담당한다. */
@Composable
internal fun AddSongScreen(
    uiState: AddSongUiState,
    onIntent: (AddSongIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBackClick: () -> Unit,
    onSongsAdded: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SongPlaylistScaffold(
            title = stringResource(R.string.afternote_editor_playlist_add_screen_title),
            onBackClick = onBackClick,
        ) { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                SelectableSongListBody(
                    songs = uiState.songs,
                    header = {
                        SongSearchSection(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { onIntent(AddSongIntent.Search(it)) },
                        )
                    },
                    initialSelectedSongKeys = emptySet(),
                    actionLabel = stringResource(R.string.afternote_add_button),
                    onAction = { selectedKeys ->
                        onSongsAdded(uiState.songs.filter { it.selectionKey in selectedKeys }.map(::toSong))
                    },
                )
                // 검색 왕복을 화면에 싣는다 (#705). 종전에는 `isLoading` 을 아무도 소비하지 않아
                // «결과 0건» 과 «아직 오는 중» 이 같은 빈 목록으로 보였다. 검색창은 그대로 두고
                // 목록 영역 위에만 표시자를 얹어, 기다리는 동안에도 질의를 고칠 수 있게 한다.
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(40.dp),
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun toSong(display: PlaylistSongDisplay): Song =
    Song(
        selectionKey = display.selectionKey,
        title = display.title,
        artist = display.artist,
        albumCoverUrl = display.albumImageUrl,
    )
