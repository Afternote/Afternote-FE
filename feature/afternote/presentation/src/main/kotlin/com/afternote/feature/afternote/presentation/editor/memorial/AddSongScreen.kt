package com.afternote.feature.afternote.presentation.editor.memorial
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.SelectableSongListBody
import com.afternote.feature.afternote.presentation.shared.detail.SongPlaylistScaffold
import com.afternote.feature.afternote.presentation.shared.detail.SongSearchSection
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * 노래 추가하기 화면 (API 검색 연동).
 *
 * 공용 부품([SongPlaylistScaffold] + selectable 본문 [SelectableSongListBody])을 직접 조립하고, 이 기능
 * 고유의 것(VM 상태·검색 실패 Snackbar·PlaylistSongDisplay↔Song 매핑)을 얹는 소비자 계층이다.
 *
 * ViewModel 의존성 없이 순수하게 UI만 그립니다. [AddSongUiState.errorRes] 는 문자열 리소스 ID 라
 * UI 레이어가 [stringResource] 로 해석한 뒤 Snackbar 표출 → [onErrorConsumed] 로 VM 에 nullify
 * 신호. VM 이 Android Framework (Context/Resources) 를 의존하지 않도록 string resolve 는 본
 * 레이어에서만 수행 (#267). 예외 원문을 실어 오던 갈래는 제거했다 — 검색 실패는 원인과 무관하게
 * 고정 안내 문구로만 노출한다 (#664). Snackbar 채택은 repo convention (Toast 2건 vs Snackbar 19건) +
 * `showSnackbar` suspend 큐 의미로 같은 리소스 ID 가 연속 발화해도 표출 누락 회피.
 */
@Composable
fun AddSongScreen(
    uiState: AddSongUiState,
    onSearchQueryChange: (String) -> Unit,
    onErrorConsumed: () -> Unit,
    onBackClick: () -> Unit,
    onSongsAdded: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnErrorConsumed by rememberUpdatedState(onErrorConsumed)
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.errorRes?.let { errorRes ->
        val message = stringResource(errorRes)
        LaunchedEffect(errorRes) {
            snackbarHostState.showSnackbar(message = message, withDismissAction = true)
            currentOnErrorConsumed()
        }
    }

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
                            onSearchQueryChange = onSearchQueryChange,
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
