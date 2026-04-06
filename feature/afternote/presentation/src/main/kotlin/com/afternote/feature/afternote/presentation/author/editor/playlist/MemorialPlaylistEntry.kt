package com.afternote.feature.afternote.presentation.author.editor.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.icon.ArrowIconSpec
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.core.ui.theme.B1
import com.afternote.core.ui.theme.B3
import com.afternote.core.ui.theme.Gray9
import com.afternote.core.ui.theme.naNumGothic
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.provider.FakeAfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteLightTheme
import com.afternote.feature.afternote.presentation.shared.DataProviderLocals
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreen
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreenManagementContent
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

data class MemorialPlaylistEntryActions(
    val onBackClick: () -> Unit = {},
    val onNavigateToAddSongScreen: () -> Unit = {},
)

/**
 * 추모 플레이리스트 Entry.
 *
 * graph-scoped [MemorialPlaylistStateHolder]의 곡 목록을 공용 [SongPlaylistScreen]의 입력 형태로 매핑합니다.
 *
 * @param playlistStateHolder graph-scoped HostViewModel에서 전달받은 플레이리스트 상태
 * @param actions 네비게이션 콜백 모음
 * @param initialSelectedSongIds Preview용. 넣으면 해당 ID가 선택된 상태로 시작 (기본 null)
 */
@Composable
fun MemorialPlaylistEntry(
    playlistStateHolder: MemorialPlaylistStateHolder,
    actions: MemorialPlaylistEntryActions = MemorialPlaylistEntryActions(),
    modifier: Modifier = Modifier,
    initialSelectedSongIds: Set<String>? = null,
) {
    val songs =
        playlistStateHolder.songs.map { s ->
            PlaylistSongDisplay(
                id = s.id,
                title = s.title,
                artist = s.artist,
                albumImageUrl = s.albumCoverUrl,
            )
        }
    SongPlaylistScreen(
        modifier = modifier,
        title = "추모 플레이리스트",
        onBackClick = actions.onBackClick,
        songs = songs,
        managementContent =
            SongPlaylistScreenManagementContent(
                leadingContent = { selectedIds ->
                    MemorialPlaylistListHeader(
                        songCount = songs.size,
                        isSelectionMode = selectedIds.isNotEmpty(),
                        onAddSongClick = actions.onNavigateToAddSongScreen,
                    )
                },
                selectionBottomBar = { selectedIds, onClearSelection ->
                    MemorialPlaylistActionBar(
                        onDeleteAllClick = {
                            playlistStateHolder.clearAllSongs()
                            onClearSelection()
                        },
                        onDeleteSelectedClick = {
                            playlistStateHolder.removeSongs(selectedIds)
                            onClearSelection()
                        },
                    )
                },
            ),
        defaultBottomNavTab = BottomNavTab.NOTE,
        initialSelectedSongIds = initialSelectedSongIds,
    )
}

/**
 * MemorialPlaylistList 화면 상단 헤더: "총 N곡" 및 "노래 추가하기" 버튼 (선택 모드가 아닐 때만 버튼 표시).
 */
@Composable
private fun MemorialPlaylistListHeader(
    songCount: Int,
    isSelectionMode: Boolean,
    onAddSongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (isSelectionMode) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "현재 플레이리스트",
                    style =
                        TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontFamily = naNumGothic,
                            fontWeight = FontWeight.Medium,
                            color = Gray9,
                        ),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Column {
                Spacer(modifier = Modifier.height(25.dp))
                Text(
                    text = "총 ${songCount}곡",
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = naNumGothic,
                            fontWeight = FontWeight.Normal,
                            color = Gray9,
                        ),
                )
                Spacer(modifier = Modifier.height(17.dp))
            }
        } else {
            Column {
                Spacer(modifier = Modifier.height(25.dp))
                Text(
                    text = "총 ${songCount}곡",
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = naNumGothic,
                            fontWeight = FontWeight.Normal,
                            color = Color(color = 0xFF000000),
                        ),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier =
                        Modifier
                            .background(
                                color = B3,
                                shape = RoundedCornerShape(20.dp),
                            ).clickable(onClick = onAddSongClick),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Text(
                                text = "노래 추가하기",
                                style =
                                    TextStyle(
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        fontFamily = naNumGothic,
                                        fontWeight = FontWeight.Medium,
                                        color = Gray9,
                                    ),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            RightArrowIcon(
                                iconSpec =
                                    ArrowIconSpec(
                                        iconRes = R.drawable.ic_arrow_right_playlist,
                                        contentDescription = "추가",
                                    ),
                                backgroundColor = B1,
                                size = 12.dp,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Spacer(modifier = Modifier.height(11.dp))
            }
        }
    }
}

@Composable
private fun MemorialPlaylistActionBar(
    onDeleteAllClick: () -> Unit,
    onDeleteSelectedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionBarShape = RoundedCornerShape(8.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 5.dp,
                    shape = actionBarShape,
                    clip = false,
                    ambientColor = Color(0x26000000),
                    spotColor = Color(0x26000000),
                ).background(color = Color.White, shape = actionBarShape)
                .clip(actionBarShape),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(onClick = onDeleteAllClick)
                    .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "전체 삭제",
                style =
                    TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontFamily = naNumGothic,
                        fontWeight = FontWeight.Normal,
                        color = Gray9,
                        textAlign = TextAlign.Center,
                    ),
            )
        }
        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0xFFE0E0E0)),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(onClick = onDeleteSelectedClick)
                    .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "선택 삭제",
                style =
                    TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontFamily = naNumGothic,
                        fontWeight = FontWeight.Normal,
                        color = Gray9,
                        textAlign = TextAlign.Center,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemorialPlaylistEntryPreview() {
    AfternoteLightTheme {
        CompositionLocalProvider(
            DataProviderLocals.LocalAfternoteEditorDataProvider provides FakeAfternoteEditorDataProvider(),
        ) {
            val provider = DataProviderLocals.LocalAfternoteEditorDataProvider.current
            val holder =
                MemorialPlaylistStateHolder().apply {
                    initializeSongs(provider.getSongs().take(3))
                }
            MemorialPlaylistEntry(
                playlistStateHolder = holder,
                actions = MemorialPlaylistEntryActions(),
            )
        }
    }
}

@Preview(showBackground = true, name = "선택 모드")
@Composable
private fun MemorialPlaylistEntrySelectionModePreview() {
    AfternoteLightTheme {
        CompositionLocalProvider(
            DataProviderLocals.LocalAfternoteEditorDataProvider provides FakeAfternoteEditorDataProvider(),
        ) {
            val provider = DataProviderLocals.LocalAfternoteEditorDataProvider.current
            val holder =
                MemorialPlaylistStateHolder().apply {
                    initializeSongs(provider.getSongs().take(4))
                }
            MemorialPlaylistEntry(
                playlistStateHolder = holder,
                actions = MemorialPlaylistEntryActions(),
                initialSelectedSongIds = setOf("1", "3"),
            )
        }
    }
}
