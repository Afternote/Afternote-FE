package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.FAB.PenFloatingActionButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.PlaylistEmptyContent
import com.afternote.feature.afternote.presentation.shared.detail.PlaylistSongList
import com.afternote.feature.afternote.presentation.shared.detail.SelectableSongListBody
import com.afternote.feature.afternote.presentation.shared.detail.SongPlaylistFloatingActionSlot
import com.afternote.feature.afternote.presentation.shared.detail.SongPlaylistScaffold
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * 추억 플레이리스트 Entry.
 *
 * flow-scoped [com.afternote.feature.afternote.presentation.editor.AfternoteEditorViewModel] 폼 SSOT의 곡 목록을
 * 공용 부품([SongPlaylistScaffold] + 모드별 본문)의 입력 형태로 매핑한다. 변경은 콜백 인텐트로 위임한다.
 *
 * 화면은 두 모드를 오간다 (헤더 연필 = 토글):
 * - 목록 모드(시안 2672:16318): 체크박스 없는 열람 목록 + 노래 추가 펜 FAB
 * - 편집 모드(시안 2672:17024): 체크박스 선택 목록 + 곡 선택 시 "전체 삭제/선택 삭제" 하단 바, FAB 숨김.
 *   시안은 미선택 상태에도 바를 그려놨지만 미선택 "선택 삭제"가 no-op 인 모순이 있어 시안 실수로
 *   판단, 선택 시 노출로 구현하고 디자이너 질의 중 (2026-07-17). 회신이 상시 노출로 확정되면
 *   selectable 본문에 상시 노출 파라미터를 되살려 스왑.
 *
 * 이 조립을 NavGraph destination 에 인라인하지 않고 Entry로 빼는 이유:
 * (1) 공용 SongPlaylist 화면 계열(shared/detail/song)은 여러 화면이 공유하는 범용 부품이라
 *     이 화면 전용 지식([Song] 도메인 매핑, 타이틀, 모드 전환·"총 N곡" 헤더·삭제 라벨·콜백)을 넣을 수 없고,
 * (2) 그 전용 지식을 NavGraph destination 블록에 인라인하면 ViewModel 없이 렌더할 수 없어
 *     Preview·스크린샷 테스트가 막힌다.
 * Entry가 둘 사이에서 도메인→표시 모델 매핑과 화면 전용 크롬 주입을 맡는 어댑터다.
 *
 * @param songs flow-scoped 에디터 폼에서 collect한 현재 곡 목록 스냅샷
 * @param onBackClick 뒤로가기
 * @param onNavigateToAddSongScreen 노래 추가 화면 진입 (목록 모드 우하단 펜 FAB)
 * @param onClearAllSongs 전체 삭제
 * @param onRemoveSongs 선택 삭제
 * @param initialEditMode 편집 모드로 시작할지 (기본 목록 모드). 내부 remember 상태의 초기값이라
 *   최초 컴포지션에만 반영되고 이후 값 변경은 무시된다 — rememberPagerState 의 initialPage 와
 *   같은 initial* 관용구. 편집 모드는 연필 클릭으로만 도달해 Preview·스크린샷이 이 진입점을 쓴다.
 * @param initialSelectedSongKeys 편집 모드에서 선택된 상태로 시작할 곡 선택 키 (기본 빈 셋).
 *   초기값 계약은 [initialEditMode] 와 동일.
 */
@Composable
fun MemorialPlaylistEntry(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onNavigateToAddSongScreen: () -> Unit,
    onClearAllSongs: () -> Unit,
    onRemoveSongs: (Set<String>) -> Unit,
    initialEditMode: Boolean = false,
    initialSelectedSongKeys: Set<String> = emptySet(),
) {
    var isEditMode by remember { mutableStateOf(initialEditMode) }
    val displaySongs =
        songs.map { s ->
            PlaylistSongDisplay(
                selectionKey = s.selectionKey,
                title = s.title,
                artist = s.artist,
                albumImageUrl = s.albumCoverUrl,
            )
        }
    SongPlaylistScaffold(
        title = stringResource(R.string.afternote_editor_playlist_screen_title),
        onBackClick = onBackClick,
        modifier = modifier,
        topBarActions = {
            IconButton(onClick = { isEditMode = !isEditMode }) {
                Icon(
                    painter = painterResource(R.drawable.afternote_ic_detail_edit),
                    contentDescription = stringResource(R.string.afternote_detail_edit),
                    modifier = Modifier.size(13.dp),
                )
            }
        },
    ) { paddingValues ->
        if (isEditMode) {
            SelectableSongListBody(
                modifier = Modifier.padding(paddingValues),
                songs = displaySongs,
                header = {
                    MemorialPlaylistListHeader(songCount = displaySongs.size)
                },
                initialSelectedSongKeys = initialSelectedSongKeys,
                actionLabel = stringResource(R.string.afternote_editor_playlist_delete_all),
                onAction = { onClearAllSongs() },
                secondaryActionLabel = stringResource(R.string.afternote_editor_playlist_delete_selected),
                onSecondaryAction = onRemoveSongs,
            )
        } else {
            Box(modifier = Modifier.padding(paddingValues)) {
                PlaylistSongList(
                    modifier = Modifier.fillMaxSize(),
                    songs = displaySongs,
                    header = {
                        MemorialPlaylistListHeader(songCount = displaySongs.size)
                    },
                )
                if (displaySongs.isEmpty()) {
                    PlaylistEmptyContent(
                        text = stringResource(R.string.afternote_editor_playlist_empty_message),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                SongPlaylistFloatingActionSlot {
                    PenFloatingActionButton(onClick = onNavigateToAddSongScreen, size = 48.dp, iconSize = 17.dp)
                }
            }
        }
    }
}

/**
 * MemorialPlaylistList 화면 상단 헤더: 모드와 무관하게 항상 "총 N곡"만 왼쪽에 표시.
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
