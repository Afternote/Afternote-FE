package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.GalleryDetailCallbacks
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.GalleryDetailScreen
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.GalleryDetailState
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.MemorialGuidelineDetailCallbacks
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.MemorialGuidelineDetailScreen
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.MemorialGuidelineDetailState
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.SocialNetworkDetailRoute
import com.afternote.feature.afternote.presentation.author.detail.model.AfternoteDeleteState
import com.afternote.feature.afternote.presentation.author.detail.model.AfternoteDetailEvent
import com.afternote.feature.afternote.presentation.author.detail.model.AfternoteDetailUiState
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover

@Composable
internal fun DetailLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun DesignPendingDetailContent(onBackClick: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(title = "", onBackClick = onBackClick)
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(Modifier.padding(paddingValues)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.design_pending))
        }
    }
}

/**
 * 삭제 결과(Succeeded/Failed) 를 감지해 뒤로가기·소비 신호를 송출하는 공용 이펙트.
 * Succeeded → onBack, 이후 VM 에 Consumed 이벤트를 전달해 상태를 Idle 로 복귀시킨다.
 */
@Composable
internal fun HandleDeleteResult(
    deleteState: AfternoteDeleteState,
    onBack: () -> Unit,
    onConsumed: () -> Unit,
) {
    LaunchedEffect(deleteState) {
        when (deleteState) {
            AfternoteDeleteState.Succeeded -> {
                onBack()
                onConsumed()
            }
            is AfternoteDeleteState.Failed -> {
                // 에러 UI 처리는 화면별 Snackbar/Dialog 에서 담당. 여기서는 상태만 소비.
                onConsumed()
            }
            AfternoteDeleteState.Idle,
            AfternoteDeleteState.InProgress,
            -> Unit
        }
    }
}

@Composable
internal fun AfternoteDetailNavigation(
    backStackEntry: NavBackStackEntry,
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
) {
    val route = backStackEntry.toRoute<AfternoteRoute.DetailRoute>()
    val afternoteId = route.itemId.toLongOrNull()
    if (afternoteId == null) {
        DesignPendingDetailContent(onBackClick = onBack)
    } else {
        SocialNetworkDetailRoute(
            afternoteId = afternoteId,
            onBack = onBack,
            onNavigateToEditor = onNavigateToEditor,
        )
    }
}

@Composable
internal fun AfternoteGalleryDetailNavigation(
    backStackEntry: NavBackStackEntry,
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
    viewModel: AfternoteDetailViewModel = hiltViewModel(),
) {
    val route = backStackEntry.toRoute<AfternoteRoute.GalleryDetailRoute>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(route.itemId) {
        route.itemId.toLongOrNull()?.let {
            viewModel.onEvent(AfternoteDetailEvent.LoadDetail(it))
        }
    }

    when (val state = uiState) {
        AfternoteDetailUiState.Loading -> DetailLoadingContent()

        is AfternoteDetailUiState.Error -> DesignPendingDetailContent(onBackClick = onBack)

        is AfternoteDetailUiState.Success -> {
            HandleDeleteResult(
                deleteState = state.deleteState,
                onBack = onBack,
                onConsumed = { viewModel.onEvent(AfternoteDetailEvent.DeleteResultConsumed) },
            )

            val detail = state.detail
            GalleryDetailScreen(
                detailState =
                    GalleryDetailState(
                        serviceName = detail.title,
                        userName = state.authorDisplayName,
                        finalWriteDate = detail.timestamps.updatedAt.ifEmpty { detail.timestamps.createdAt },
                        afternoteEditReceivers =
                            detail.receivers.map { r ->
                                AfternoteEditorReceiver(
                                    id = "",
                                    name = r.name,
                                    label = r.relation,
                                )
                            },
                        informationProcessingMethod = detail.processing?.method ?: "",
                        processingMethods = detail.processing?.actions ?: emptyList(),
                        message = detail.processing?.leaveMessage ?: "",
                    ),
                callbacks =
                    GalleryDetailCallbacks(
                        onBackClick = onBack,
                        onEditClick = {
                            onNavigateToEditor(detail.id.toString())
                        },
                        onDeleteConfirm = { viewModel.onEvent(AfternoteDetailEvent.Delete(detail.id)) },
                    ),
            )
        }
    }
}

@Composable
internal fun AfternoteMemorialGuidelineDetailNavigation(
    backStackEntry: NavBackStackEntry,
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
    viewModel: AfternoteDetailViewModel = hiltViewModel(),
) {
    val route = backStackEntry.toRoute<AfternoteRoute.MemorialGuidelineDetailRoute>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(route.itemId) {
        route.itemId.toLongOrNull()?.let {
            viewModel.onEvent(AfternoteDetailEvent.LoadDetail(it))
        }
    }

    when (val state = uiState) {
        AfternoteDetailUiState.Loading -> DetailLoadingContent()

        is AfternoteDetailUiState.Error -> DesignPendingDetailContent(onBackClick = onBack)

        is AfternoteDetailUiState.Success -> {
            HandleDeleteResult(
                deleteState = state.deleteState,
                onBack = onBack,
                onConsumed = { viewModel.onEvent(AfternoteDetailEvent.DeleteResultConsumed) },
            )

            val detail = state.detail
            MemorialGuidelineDetailScreen(
                detailState =
                    MemorialGuidelineDetailState(
                        userName = state.authorDisplayName,
                        finalWriteDate = detail.timestamps.updatedAt.ifEmpty { detail.timestamps.createdAt },
                        profileImageUri = detail.playlist?.playlistDetailMemorialMedia?.photoUrl,
                        afternoteEditReceivers =
                            detail.receivers.map { r ->
                                AfternoteEditorReceiver(
                                    id = "",
                                    name = r.name,
                                    label = r.relation,
                                )
                            },
                        albumCovers =
                            detail.playlist?.songs?.map { s ->
                                AlbumCover(
                                    id = (s.id ?: 0L).toString(),
                                    imageUrl = s.coverUrl,
                                    title = s.title,
                                )
                            } ?: emptyList(),
                        songCount = detail.playlist?.songs?.size ?: 0,
                        lastWish = detail.playlist?.atmosphere ?: "",
                        memorialVideoUrl = detail.playlist?.playlistDetailMemorialMedia?.videoUrl,
                        memorialThumbnailUrl = detail.playlist?.playlistDetailMemorialMedia?.thumbnailUrl,
                    ),
                callbacks =
                    MemorialGuidelineDetailCallbacks(
                        onBackClick = onBack,
                        onEditClick = {
                            onNavigateToEditor(detail.id.toString())
                        },
                        onDeleteConfirm = { viewModel.onEvent(AfternoteDetailEvent.Delete(detail.id)) },
                    ),
            )
        }
    }
}
