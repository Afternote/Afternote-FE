package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

// region ── PlaylistSongList (list-level composable) ──

/**
 * 노래 목록 렌더러 (헤더 슬롯 + 행들).
 * 소비자 화면이 [SongPlaylistScaffold] 안에서 직접 쓰거나, 선택 본문 [SelectableSongListBody] 를 통해 쓴다.
 *
 * 이 리스트는 필터도 검색도 소유하지 않는다 — [songs] 를 받은 그대로 그리고, 상단 헤더는 호출부가
 * [header] 로 주입한다(필수 — 모든 호출부가 헤더를 가진다). 곡을 골라 담는 화면(AddSong)은
 * [SongSearchSection] 을 헤더로 주입하고, 열람·관리 화면은 "총 N곡" 헤더를 주입한다 — 즉 검색은 "여러
 * 헤더 중 하나"일 뿐 리스트의 고정 지식이 아니다. 리스트가 내부 필터·검색을 갖던 시절엔 API 결과
 * 이중 필터·죽은 검색 바인딩 문제가 있어 둘 다 호출부로 넘겼다 (2026-07).
 * 구명 SearchableSongList — addsong 패키지 태생이라 "추가용 검색" 함의가 남아
 * [PlaylistSongDisplay]·[PlaylistSongItem] 가족 명명으로 개명 (2026-07).
 *
 * @param songs 표시할 노래 목록 (호출부가 이미 필터링한 최종 목록 — 리스트는 그대로 그린다)
 * @param onSongClick 노래 행 클릭 콜백 (null이면 비클릭)
 * @param isSelected 각 행의 선택 상태 판정 (null이면 체크박스 없음 = 비선택 모드; 예: view-only 열람)
 * @param header 첫 아이템으로 그릴 헤더 (검색창·"총 N곡" 등). 필수 — 현재 모든 호출부가 헤더를 가진다.
 */
@Composable
fun PlaylistSongList(
    modifier: Modifier = Modifier,
    songs: List<PlaylistSongDisplay>,
    onSongClick: ((PlaylistSongDisplay) -> Unit)? = null,
    isSelected: ((PlaylistSongDisplay) -> Boolean)? = null,
    header: @Composable () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    LazyColumn(
        modifier =
            modifier
                .fillMaxWidth()
                .addFocusCleaner(focusManager),
        // side inset 은 앱 전 화면 공통 20dp 고정 — 다른 값이 필요한 호출부가 생기면 그때 파라미터로 승격.
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        // 헤더 = 호출부가 주입한 조각(검색창·"총 N곡" 등). 필수라 항상 첫 아이템으로 렌더.
        // 헤더-리스트 8dp 간격은 별도 Spacer 아이템이 아니라 헤더 아이템의 bottom 패딩으로 준다 —
        // 값을 여기 한 곳에만 두고(각 헤더에 중복 X), LazyColumn 에 빈 유령 아이템도 안 만든다.
        item { Box(Modifier.padding(bottom = 8.dp)) { header() } }
        itemsIndexed(songs) { _, song ->
            PlaylistSongItem(
                song = song,
                onClick = onSongClick,
                selected = isSelected?.invoke(song),
            )
        }
    }
}

// endregion

// region ── Private sub-components ──

/**
 * hoisted String ↔ [TextFieldState] 양방향 바인딩 팩토리.
 *
 * [SongSearchSection] 의 레이아웃(label·입력창) 사이에 끼어 있던 상태 블록을 뽑아낸 것 — 호출부/VM 이
 * 소유한 [searchQuery] 문자열과 Compose 가 소유하는 [TextFieldState] 를 잇는다: 밖→안(외부 쿼리 변경을
 * 필드에 반영)은 [searchQuery] 키 [LaunchedEffect] 로, 안→밖(타이핑을 콜백으로)은 [snapshotFlow] 로.
 * UI 레이어가 [TextFieldState] 를 소유하고 VM 은 String 만 주고받는 경계를 지킨다.
 */
@Composable
private fun rememberSongSearchFieldState(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
): TextFieldState {
    val state = rememberTextFieldState(searchQuery)
    LaunchedEffect(searchQuery) {
        if (state.text.toString() != searchQuery) {
            state.edit { replace(0, length, searchQuery) }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { state.text.toString() }
            .collect { onSearchQueryChange(it) }
    }
    return state
}

/**
 * 곡 검색 헤더 (label + 입력창). [PlaylistSongList] 의 헤더 슬롯에 주입해 쓰는 여러 헤더 중 하나 —
 * view-only(수신자 열람)·selectable(노래 추가)이 주입한다. 관리 모드는 대신 "총 N곡" 헤더를 주입한다.
 */
@Composable
fun SongSearchSection(
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
                .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = stringResource(R.string.afternote_song_search_label),
            style =
                AfternoteDesign.typography.bodyBase.copy(
                    color = AfternoteDesign.colors.gray9,
                ),
        )
        val searchFieldState = rememberSongSearchFieldState(searchQuery, onSearchQueryChange)
        AfternoteTextField(
            state = searchFieldState,
            type = TextFieldType.Search,
            placeholder = stringResource(R.string.afternote_song_search_placeholder),
            imeAction = ImeAction.Search,
        )
    }
}

// endregion
