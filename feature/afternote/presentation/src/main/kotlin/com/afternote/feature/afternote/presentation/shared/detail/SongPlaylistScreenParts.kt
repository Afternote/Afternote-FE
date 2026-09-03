package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * 세 화면 공통 껍데기: 투명 배경 Scaffold + DetailTopBar (뒤로가기 시 포커스 해제).
 * [content] 슬롯에 각 모드의 본문(리스트 / 선택 본문)을 넣는다.
 *
 * @param topBarActions [DetailTopBar] 우측 액션 슬롯 (예: 관리 화면의 편집 모드 토글 연필)
 */
@Composable
fun SongPlaylistScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val focusManager = LocalFocusManager.current
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
                actions = topBarActions,
            )
        },
        content = content,
    )
}

/**
 * 플레이리스트 화면 계열 공용 부유 액션 배치 슬롯.
 *
 * 시안 실측(목록 2672:16318): FAB 는 end 22/bottom 72 — 하단 액션 바(사이드 20/bottom 54)와
 * 다른 오프셋을 쓴다. 바 값을 재사용하지 말 것. [SelectableSongListBody] 내부와 목록 모드처럼
 * 리스트를 직접 조립하는 호출부가 같은 오프셋을 공유하도록 여기 한 곳에만 둔다.
 */
@Composable
internal fun BoxScope.SongPlaylistFloatingActionSlot(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 72.dp),
    ) {
        content()
    }
}

/**
 * selectable·management 공통 본문: 선택 상태(selectedSongKeys)를 소유하고 체크박스 목록 + 선택 시
 * 하단에 [AfternoteButton] 액션을 그린다. 두 모드의 차이는 [header] 와 액션 파라미터뿐이다.
 *
 * 액션은 항상 "실행 → 선택 초기화 → 화면 유지"라 동작이 같아, 클릭 시 본문이 콜백을 부른 뒤 선택을
 * 스스로 비운다 (호출부는 clear 를 몰라도 된다). [onSecondaryAction] 이 있으면 dual-action Variant5
 * (전체/선택 삭제), 없으면 단일 Default("추가하기") 로 렌더한다.
 *
 * @param header 목록 첫 아이템 헤더 (검색창 / "총 N곡")
 * @param actionLabel 하단 버튼 라벨 (dual-action 이면 왼쪽 라벨)
 * @param onAction 버튼(또는 dual 왼쪽) 클릭 — 현재 선택된 키 집합을 받는다
 * @param secondaryActionLabel dual-action 오른쪽 라벨 (null 이면 단일 버튼)
 * @param onSecondaryAction dual-action 오른쪽 클릭 — 현재 선택된 키 집합을 받는다
 * @param floatingActionButton 선택이 비었을 때만 우하단에 노출되는 부유 액션(선택 중엔 하단 액션 버튼이 대신 노출)
 */
@Composable
internal fun SelectableSongListBody(
    songs: List<PlaylistSongDisplay>,
    header: @Composable () -> Unit,
    initialSelectedSongKeys: Set<String>,
    actionLabel: String,
    onAction: (selectedKeys: Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryAction: ((selectedKeys: Set<String>) -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
) {
    var selectedSongKeys by remember { mutableStateOf(initialSelectedSongKeys) }
    Box(modifier = modifier) {
        PlaylistSongList(
            modifier = Modifier.fillMaxSize(),
            songs = songs,
            onSongClick = { song ->
                selectedSongKeys =
                    if (song.selectionKey in selectedSongKeys) {
                        selectedSongKeys - song.selectionKey
                    } else {
                        selectedSongKeys + song.selectionKey
                    }
            },
            isSelected = { song -> song.selectionKey in selectedSongKeys },
            header = header,
        )
        if (selectedSongKeys.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 20.dp, end = 20.dp, bottom = 54.dp),
            ) {
                AfternoteButton(
                    text = actionLabel,
                    onClick = {
                        onAction(selectedSongKeys)
                        selectedSongKeys = emptySet()
                    },
                    type =
                        if (onSecondaryAction != null) {
                            AfternoteButtonType.Variant5
                        } else {
                            AfternoteButtonType.Default
                        },
                    secondaryText = secondaryActionLabel,
                    onSecondaryClick =
                        onSecondaryAction?.let { secondary ->
                            {
                                secondary(selectedSongKeys)
                                selectedSongKeys = emptySet()
                            }
                        },
                )
            }
        }
        if (floatingActionButton != null && selectedSongKeys.isEmpty()) {
            SongPlaylistFloatingActionSlot { floatingActionButton() }
        }
    }
}
