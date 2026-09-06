package com.afternote.feature.afternote.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.modifierextention.FadingEdgeDirection
import com.afternote.core.ui.modifierextention.horizontalFadingEdge
import com.afternote.core.ui.popup.AfternoteActionMenu
import com.afternote.core.ui.popup.editDeleteActionMenuItems
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.InfoCard
import com.afternote.feature.afternote.presentation.shared.detail.MemorialVideoThumbnail
import com.afternote.feature.afternote.presentation.shared.detail.ReceiversCard
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

/**
 * 추억 노트 상세 표시 데이터.
 */
@Immutable
data class MemorialDetailContent(
    val finalWriteDate: String = "",
    val profileImageUri: String? = null,
    val albumCovers: List<AlbumCover> = emptyList(),
    val songCount: Int = 0,
    val afternoteEditReceivers: List<ReceiverUiModel> = emptyList(),
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
)

/**
 * 추억 노트 애프터노트 상세 화면 (Stateless).
 *
 * [com.afternote.feature.afternote.presentation.detail.account.AccountDetailScreen] 과 동일한 Scaffold·TopBar·드롭다운 배치·스크롤 modifier 패턴을 따른다.
 */
@Composable
fun MemorialDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: MemorialDetailContent = MemorialDetailContent(),
    userName: String = "",
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    isEditable: Boolean = true,
    onEditClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onVideoClick: (String) -> Unit,
    state: AfternoteDetailState = rememberAfternoteDetailState(),
) {
    val memorialCategoryLabel = stringResource(R.string.afternote_category_memorial)

    if (isEditable && state.showDeleteDialog) {
        DeleteConfirmDialog(
            serviceName = memorialCategoryLabel,
            onDismiss = state::hideDeleteDialog,
            onConfirm = {
                state.hideDeleteDialog()
                onDeleteConfirm()
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.afternote_detail_title),
                onBackClick = onBackClick,
                actions = {
                    if (isEditable) {
                        Box {
                            IconButton(onClick = state::toggleDropdownMenu) {
                                Icon(
                                    painter = painterResource(R.drawable.afternote_ic_detail_edit),
                                    contentDescription = stringResource(R.string.afternote_detail_edit),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            AfternoteActionMenu(
                                expanded = state.showDropdownMenu,
                                onDismissRequest = state::hideDropdownMenu,
                                items =
                                    editDeleteActionMenuItems(
                                        onEditClick = onEditClick,
                                        onDeleteClick = { state.showDeleteDialog() },
                                    ),
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        MemorialDetailScrollContent(
            content = content,
            categoryLabel = memorialCategoryLabel,
            userName = userName,
            onVideoClick = onVideoClick,
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        )
    }
}

@Composable
private fun MemorialDetailScrollContent(
    content: MemorialDetailContent,
    categoryLabel: String,
    userName: String,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        TitleSection(categoryLabel = categoryLabel, userName = userName)
        Spacer(modifier = Modifier.height(24.dp))
        CardSection(content = content, onVideoClick = onVideoClick)
        Spacer(modifier = Modifier.height(24.dp))
        SharingNotice()
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TitleSection(
    categoryLabel: String,
    userName: String,
) {
    // 프로필 로드 실패·로딩 경합으로 이름이 비어 있으면 이름 세그먼트를 생략해
    // "…에 대한 님의 기록" 렌더를 막는다.
    val titleSuffix =
        if (userName.isBlank()) {
            stringResource(R.string.afternote_memorial_detail_title_suffix_no_name)
        } else {
            stringResource(R.string.afternote_memorial_detail_title_suffix, userName)
        }
    Text(
        text =
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = AfternoteDesign.colors.gray9)) {
                    append(categoryLabel)
                }
                append(titleSuffix)
            },
        style = AfternoteDesign.typography.bodyLargeB,
    )
}

@Composable
private fun CardSection(
    content: MemorialDetailContent,
    onVideoClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PhotoCard(
            finalWriteDate = content.finalWriteDate,
            profileImageUri = content.profileImageUri,
        )
        ReceiversCard(receivers = content.afternoteEditReceivers)
        PlaylistCard(
            albumCovers = content.albumCovers,
            songCount = content.songCount,
        )
        VideoCard(
            videoUrl = content.memorialVideoUrl,
            thumbnailUrl = content.memorialThumbnailUrl,
            onClick = onVideoClick,
        )
    }
}

@Composable
private fun SharingNotice() {
    InfoCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "•",
                style =
                    AfternoteDesign.typography.bodySmallR.copy(
                        color = AfternoteDesign.colors.gray6,
                    ),
            )
            Text(
                text = stringResource(R.string.afternote_memorial_detail_sharing_notice),
                style =
                    AfternoteDesign.typography.bodySmallR.copy(
                        color = AfternoteDesign.colors.gray6,
                    ),
            )
        }
    }
}

@Composable
private fun PhotoCard(
    finalWriteDate: String,
    profileImageUri: String?,
) {
    InfoCard(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.afternote_last_written_date, finalWriteDate),
                    modifier = Modifier.fillMaxWidth(),
                    style =
                        AfternoteDesign.typography.footnoteCaption.copy(
                            color = AfternoteDesign.colors.gray6,
                        ),
                )
                ProfileImage(
                    displayImageUri = profileImageUri,
                )
            }
        },
    )
}

/**
 * 장례식에 남길 영상 카드 — 정본 시안
 * [node 4327:72859](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-72859) 기준.
 *
 * InfoCard(AfternoteDesign.colors.gray2) 안에 제목 + 썸네일([MemorialVideoThumbnail]) 구조.
 * 영상 URL이 없으면 카드를 표시하지 않는다.
 *
 * 종전 KDoc 이 근거로 든 node 4813:15198 은 2026-08-23 파일 재편으로 사라져 조회되지 않는다 (#463).
 */
@Composable
private fun VideoCard(
    videoUrl: String?,
    thumbnailUrl: String?,
    onClick: (String) -> Unit,
) {
    if (videoUrl.isNullOrBlank()) return

    InfoCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button) { onClick(videoUrl) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.afternote_editor_funeral_video_label),
                    style =
                        AfternoteDesign.typography.textField.copy(
                            fontWeight = FontWeight.Medium,
                            color = AfternoteDesign.colors.gray9,
                        ),
                )
                MemorialVideoThumbnail(thumbnailUrl = thumbnailUrl)
            }
        },
    )
}

/**
 * 추억 플레이리스트 카드 — 피그마 node 4160:9168 기준.
 *
 * 레이아웃 순서: 제목 → 앨범 커버 행 → 곡 수 텍스트.
 * InfoCard(AfternoteDesign.colors.gray2) 안에 직접 렌더링하며, 내부 AfternoteDesign.colors.white 카드 없이 flat 구조.
 * 앨범 커버: 87dp, 간격 10dp, 오른쪽 45dp 페이드.
 */
@Composable
private fun PlaylistCard(
    albumCovers: List<AlbumCover>,
    songCount: Int,
) {
    InfoCard(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.afternote_editor_playlist_screen_title),
                    style =
                        AfternoteDesign.typography.textField.copy(
                            fontWeight = FontWeight.Medium,
                            color = AfternoteDesign.colors.gray9,
                        ),
                )
                Spacer(Modifier.height(7.dp))
                if (albumCovers.isNotEmpty()) {
                    PlaylistAlbumRow(albumCovers = albumCovers)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.afternote_detail_playlist_song_count, songCount),
                    style =
                        AfternoteDesign.typography.bodySmallR.copy(
                            color = AfternoteDesign.colors.black,
                        ),
                )
            }
        },
    )
}

@Composable
private fun PlaylistAlbumRow(albumCovers: List<AlbumCover>) {
    val listState = rememberLazyListState()
    val needsHorizontalFade by remember {
        derivedStateOf {
            listState.canScrollBackward || listState.canScrollForward
        }
    }
    val fadingDirection by remember {
        derivedStateOf {
            when {
                listState.canScrollBackward && listState.canScrollForward -> {
                    FadingEdgeDirection.BOTH
                }

                listState.canScrollBackward -> {
                    FadingEdgeDirection.LEFT
                }

                listState.canScrollForward -> {
                    FadingEdgeDirection.RIGHT
                }

                else -> {
                    FadingEdgeDirection.RIGHT
                }
            }
        }
    }
    LazyRow(
        state = listState,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (needsHorizontalFade) {
                        Modifier.horizontalFadingEdge(
                            edgeWidth = 45.dp,
                            direction = fadingDirection,
                        )
                    } else {
                        Modifier
                    },
                ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(albumCovers) { _, album ->
            AlbumCoverItem(album = album)
        }
    }
}

@Composable
private fun AlbumCoverItem(album: AlbumCover) {
    if (!album.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = album.imageUrl,
            contentDescription = album.title,
            modifier = Modifier.size(87.dp),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier =
                Modifier
                    .size(87.dp)
                    .background(
                        color = AfternoteDesign.colors.gray3,
                        shape = RoundedCornerShape(8.dp),
                    ),
        )
    }
}
