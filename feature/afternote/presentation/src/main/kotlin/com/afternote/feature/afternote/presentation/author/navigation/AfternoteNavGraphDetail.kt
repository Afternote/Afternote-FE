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
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.GalleryDetailCallbacks
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.GalleryDetailScreen
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.GalleryDetailState
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.MemorialGuidelineDetailCallbacks
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.MemorialGuidelineDetailScreen
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.MemorialGuidelineDetailState
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.SocialNetworkDetailContent
import com.afternote.feature.afternote.presentation.author.detail.afternotedetail.SocialNetworkDetailScreen
import com.afternote.feature.afternote.presentation.author.detail.model.AfternoteDetailEvent
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.util.getIconResForServiceName

private val designedDetailTypes = setOf(AfternoteServiceType.SOCIAL_NETWORK)

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

@Composable
internal fun AfternoteDetailNavigation(
    backStackEntry: NavBackStackEntry,
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
    viewModel: AfternoteDetailViewModel = hiltViewModel(),
) {
    val route = backStackEntry.toRoute<AfternoteRoute.DetailRoute>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(route.itemId) {
        route.itemId.toLongOrNull()?.let {
            viewModel.onEvent(AfternoteDetailEvent.LoadDetail(it))
        }
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onBack()
        }
    }

    val detail = uiState.detail

    when {
        uiState.isLoading -> {
            DetailLoadingContent()
        }

        detail == null || detail.type !in designedDetailTypes -> {
            DesignPendingDetailContent(onBackClick = onBack)
        }

        else -> {
            val method = detail.processing?.method ?: ""
            val badgeResId =
                when {
                    method.contains("TRANSFER") -> R.string.feature_afternote_detail_receiver_info_transfer_badge
                    else -> R.string.feature_afternote_detail_receiver_assigned
                }

            SocialNetworkDetailScreen(
                content =
                    SocialNetworkDetailContent(
                        serviceName = detail.title,
                        userName = uiState.authorDisplayName,
                        accountId = detail.credentials?.id ?: "",
                        password = detail.credentials?.password ?: "",
                        accountProcessingMethod = method,
                        processingMethods = detail.processing?.actions ?: emptyList(),
                        message = detail.processing?.leaveMessage ?: "",
                        finalWriteDate = detail.timestamps.updatedAt.ifEmpty { detail.timestamps.createdAt },
                        afternoteEditReceivers =
                            detail.receivers.map { r ->
                                AfternoteEditorReceiver(
                                    id = "",
                                    name = r.name,
                                    label = r.relation,
                                )
                            },
                        iconResId = getIconResForServiceName(detail.title),
                        badgeTextResId = badgeResId,
                    ),
                onBackClick = onBack,
                onEditClick = {
                    onNavigateToEditor(detail.id.toString())
                },
                onDeleteConfirm = { viewModel.onEvent(AfternoteDetailEvent.Delete(detail.id)) },
            )
        }
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

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onBack()
        }
    }

    val detail = uiState.detail

    when {
        uiState.isLoading -> {
            DetailLoadingContent()
        }

        detail == null -> {
            DesignPendingDetailContent(
                onBackClick = onBack,
            )
        }

        else -> {
            GalleryDetailScreen(
                detailState =
                    GalleryDetailState(
                        serviceName = detail.title,
                        userName = uiState.authorDisplayName,
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

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onBack()
        }
    }

    val detail = uiState.detail

    when {
        uiState.isLoading -> {
            DetailLoadingContent()
        }

        detail == null -> {
            DesignPendingDetailContent(
                onBackClick = onBack,
            )
        }

        else -> {
            MemorialGuidelineDetailScreen(
                detailState =
                    MemorialGuidelineDetailState(
                        userName = uiState.authorDisplayName,
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
