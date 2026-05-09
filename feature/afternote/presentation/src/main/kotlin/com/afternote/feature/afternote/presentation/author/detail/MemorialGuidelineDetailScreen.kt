package com.afternote.feature.afternote.presentation.author.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.afternote.core.model.AlbumCover
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.modifierextention.FadingEdgeDirection
import com.afternote.core.ui.modifierextention.horizontalFadingEdge
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.navigation.DesignPendingDetailContent
import com.afternote.feature.afternote.presentation.author.navigation.DetailLoadingContent
import com.afternote.feature.afternote.presentation.author.navigation.HandleDeleteResult
import com.afternote.feature.afternote.presentation.shared.detail.DeleteConfirmDialog
import com.afternote.feature.afternote.presentation.shared.detail.EditDropdownMenu
import com.afternote.feature.afternote.presentation.shared.detail.InfoCard
import com.afternote.feature.afternote.presentation.shared.detail.ReceiversCard
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel
import com.afternote.core.ui.R as CoreUiR

/**
 * 추모 가이드라인 상세 Stateful Route.
 *
 * [com.afternote.feature.afternote.presentation.author.detail.socialnetwork.SocialNetworkDetailRoute]·[GalleryDetailRoute] 와 동일한 VM·UiState·삭제 이펙트 패턴을 따르며,
 * 성공 시 [AfternoteDetailUiState.Success.contentUiModel] 이 추모가 아니면 폴백한다.
 */
@Composable
internal fun MemorialGuidelineDetailRoute(
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
    viewModel: AfternoteDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        AfternoteDetailUiState.Loading -> {
            DetailLoadingContent()
        }

        is AfternoteDetailUiState.Error -> {
            DesignPendingDetailContent(onBackClick = onBack)
        }

        is AfternoteDetailUiState.Success -> {
            HandleDeleteResult(
                deleteState = state.deleteState,
                onBack = onBack,
                onConsumed = viewModel::consumeDeleteResult,
            )

            when (val model = state.contentUiModel) {
                is DetailContentUiModel.Memorial -> {
                    MemorialGuidelineDetailScreen(
                        content = model.content,
                        onBackClick = onBack,
                        onEditClick = { onNavigateToEditor(state.detailId.toString()) },
                        onDeleteConfirm = { viewModel.deleteAfternote(state.detailId) },
                    )
                }

                else -> {
                    DesignPendingDetailContent(onBackClick = onBack)
                }
            }
        }
    }
}

/**
 * 추모 가이드라인 상세 표시 데이터.
 */
@Immutable
data class MemorialGuidelineDetailContent(
    val userName: String = "서영",
    val finalWriteDate: String = "2025.11.26.",
    val profileImageUri: String? = null,
    val albumCovers: List<AlbumCover> = emptyList(),
    val songCount: Int = 0,
    val lastWish: String = "",
    val afternoteEditReceivers: List<ReceiverUiModel> = emptyList(),
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
)

/**
 * 추모 가이드라인 애프터노트 상세 화면 (Stateless).
 *
 * [com.afternote.feature.afternote.presentation.author.detail.socialnetwork.SocialNetworkDetailScreen] 과 동일한 Scaffold·TopBar·드롭다운 배치·스크롤 modifier 패턴을 따른다.
 */
@Composable
fun MemorialGuidelineDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: MemorialGuidelineDetailContent = MemorialGuidelineDetailContent(),
    isEditable: Boolean = true,
    onEditClick: () -> Unit = {},
    onDeleteConfirm: () -> Unit = {},
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
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.feature_afternote_detail_title),
                onBackClick = onBackClick,
                actions = {
                    if (isEditable) {
                        Box {
                            IconButton(onClick = state::toggleDropdownMenu) {
                                Icon(
                                    painter = painterResource(R.drawable.afternote_ui_detail_edit),
                                    contentDescription = stringResource(R.string.feature_afternote_detail_edit),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            EditDropdownMenu(
                                expanded = state.showDropdownMenu,
                                onDismissRequest = state::hideDropdownMenu,
                                onDeleteClick = { state.showDeleteDialog() },
                                onEditClick = onEditClick,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        MemorialGuidelineDetailScrollContent(
            content = content,
            categoryLabel = memorialCategoryLabel,
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        )
    }
}

@Composable
private fun MemorialGuidelineDetailScrollContent(
    content: MemorialGuidelineDetailContent,
    categoryLabel: String,
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
        TitleSection(categoryLabel = categoryLabel, userName = content.userName)
        Spacer(modifier = Modifier.height(24.dp))
        CardSection(content = content)
    }
}

@Composable
private fun TitleSection(
    categoryLabel: String,
    userName: String,
) {
    Text(
        text =
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = AfternoteDesign.colors.gray9)) {
                    append(categoryLabel)
                }
                append("에 대한 ${userName}님의 기록")
            },
        style = AfternoteDesign.typography.bodyLargeB,
    )
}

@Composable
private fun CardSection(content: MemorialGuidelineDetailContent) {
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
        LastWishCard(lastWish = content.lastWish)
        VideoCard(
            videoUrl = content.memorialVideoUrl,
            thumbnailUrl = content.memorialThumbnailUrl,
        )
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
                    text = "최종 작성일 $finalWriteDate",
                    modifier = Modifier.fillMaxWidth(),
                    style =
                        AfternoteDesign.typography.footnoteCaption.copy(
                            color = AfternoteDesign.colors.gray6,
                        ),
                )
                ProfileImage(
                    onClick = {}, // TODO:
                    isEditable = false,
                    displayImageUri = profileImageUri,
                )
            }
        },
    )
}

/**
 * 장례식에 남길 영상 카드 — 피그마 node 4813:15198 기준.
 *
 * InfoCard(AfternoteDesign.colors.gray2) 안에 제목 + 썸네일(dark gradient overlay + 재생 아이콘) 구조.
 * 영상 URL이 없으면 카드를 표시하지 않는다.
 */
@Composable
private fun VideoCard(
    videoUrl: String?,
    thumbnailUrl: String?,
) {
    if (videoUrl.isNullOrBlank()) return

    InfoCard(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "장례식에 남길 영상",
                    style =
                        AfternoteDesign.typography.textField.copy(
                            fontWeight = FontWeight.Medium,
                            color = AfternoteDesign.colors.gray9,
                        ),
                )
                VideoThumbnail(thumbnailUrl = thumbnailUrl)
            }
        },
    )
}

/**
 * 영상 썸네일 + 다크 그라데이션 오버레이 + 재생 아이콘.
 *
 * 피그마 기준: 높이 183dp, 16dp radius, gradient 0x99757575 → 0x99222222, 재생 아이콘 32dp 중앙.
 */
@Composable
private fun VideoThumbnail(thumbnailUrl: String?) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(183.dp)
                .clip(RoundedCornerShape(16.dp)),
    ) {
        // 썸네일 이미지
        if (!thumbnailUrl.isNullOrBlank()) {
            val context = LocalContext.current
            val imageRequest =
                remember(thumbnailUrl) {
                    ImageRequest
                        .Builder(context)
                        .data(thumbnailUrl)
                        .httpHeaders(
                            NetworkHeaders
                                .Builder()
                                .apply {
                                    this["User-Agent"] = "Afternote Android App"
                                }.build(),
                        ).build()
                }
            AsyncImage(
                model = imageRequest,
                contentDescription = "장례식에 남길 영상 썸네일",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // 다크 그라데이션 오버레이
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        AfternoteDesign.colors.gray6.copy(alpha = 153f / 255f),
                                        AfternoteDesign.colors.gray9.copy(alpha = 153f / 255f),
                                    ),
                            ),
                    ),
        )

        // 재생 아이콘
        Image(
            painter = painterResource(R.drawable.feature_afternote_ic_playback),
            contentDescription = "영상 재생",
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(32.dp),
        )
    }
}

/**
 * 추모 플레이리스트 카드 — 피그마 node 4160:9168 기준.
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
                    text = "추모 플레이리스트",
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
                    text = "현재 ${songCount}개의 노래가 담겨 있습니다.",
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
        val context = LocalContext.current
        val imageRequest =
            remember(album.imageUrl) {
                ImageRequest
                    .Builder(context)
                    .data(album.imageUrl)
                    .httpHeaders(
                        NetworkHeaders
                            .Builder()
                            .apply {
                                this["User-Agent"] = "Afternote Android App"
                            }.build(),
                    ).build()
            }
        AsyncImage(
            model = imageRequest,
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

@Composable
private fun LastWishCard(lastWish: String) {
    val displayText =
        lastWish.ifEmpty { stringResource(CoreUiR.string.core_ui_last_wish_empty_state) }
    val textColor =
        if (lastWish.isNotEmpty()) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray5

    InfoCard(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Column {
                Text(
                    text = stringResource(CoreUiR.string.core_ui_label_last_wish),
                    style =
                        AfternoteDesign.typography.textField.copy(
                            fontWeight = FontWeight.Medium,
                            color = AfternoteDesign.colors.gray9,
                        ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayText,
                    style =
                        AfternoteDesign.typography.bodySmallR.copy(
                            color = textColor,
                        ),
                )
            }
        },
    )
}

private fun memorialGuidelineDetailPreviewAlbumCovers(): List<AlbumCover> =
    listOf(
        AlbumCover(id = "1"),
        AlbumCover(id = "2"),
        AlbumCover(id = "3"),
        AlbumCover(id = "4"),
    )

@Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=420,isRound=false",
)
@Composable
private fun MemorialGuidelineDetailScreenPreview() {
    AfternoteTheme {
        MemorialGuidelineDetailScreen(
            content =
                MemorialGuidelineDetailContent(
                    songCount = 16,
                    albumCovers = memorialGuidelineDetailPreviewAlbumCovers(),
                    lastWish = "차분하고 조용하게 보내주세요.",
                ),
            onBackClick = {},
            onEditClick = {},
        )
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp,dpi=420,isRound=false",
    name = "Memorial Guideline Detail - Delete Dialog",
)
@Composable
private fun MemorialGuidelineDetailScreenDeleteDialogPreview() {
    AfternoteTheme {
        val stateWithDialog =
            remember {
                AfternoteDetailState().apply {
                    showDeleteDialog()
                }
            }
        MemorialGuidelineDetailScreen(
            content =
                MemorialGuidelineDetailContent(
                    songCount = 16,
                    albumCovers = memorialGuidelineDetailPreviewAlbumCovers(),
                    lastWish = "차분하고 조용하게 보내주세요.1",
                ),
            onBackClick = {},
            onEditClick = {},
            state = stateWithDialog,
        )
    }
}
