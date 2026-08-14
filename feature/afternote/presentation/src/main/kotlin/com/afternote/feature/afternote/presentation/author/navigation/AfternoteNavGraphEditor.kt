package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreen
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreenCallbacks
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternotePayloadBuilder
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorUiState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.rememberAfternoteEditorState
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.author.navigation.model.SELECTED_RECEIVER_ID_KEY

/**
 * 작성자 에디터 플로우: type-safe [AfternoteRoute.EditorRoute] + 단방향 이벤트.
 *
 * 홈의 `visibleItems` 스냅샷은 에디터에 전달하지 않는다. 식별은 라우트의 `itemId`·`initialCategory` 정도로 최소화한다.
 *
 * **수정 진입 데이터 로드:** 상세 화면과 같이 [com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel]의 `init`에서
 * [androidx.lifecycle.SavedStateHandle]의 `itemId`만 보고 Repository `getDetail`을 호출한다 (Compose `LaunchedEffect` 위임 없음).
 */
internal sealed class EditorSaveErrorResult {
    data class Validation(
        val messageResId: Int,
    ) : EditorSaveErrorResult()

    data class Generic(
        val messageResId: Int,
    ) : EditorSaveErrorResult()
}

internal fun editorSaveErrorFromUiState(
    uiState: AfternoteEditorUiState,
    playlistSongCount: Int,
): EditorSaveErrorResult? {
    if (uiState.validationError == AfternoteValidationError.PLAYLIST_SONGS_REQUIRED &&
        playlistSongCount > 0
    ) {
        return null
    }
    uiState.validationError?.let { return EditorSaveErrorResult.Validation(it.messageResId) }
    uiState.errorRes?.let { return EditorSaveErrorResult.Generic(it) }
    return null
}

internal data class AfternoteEditorNavigationParams(
    val backStackEntry: NavBackStackEntry,
    val liveSongs: List<Song>,
    val onReplaceSongs: (List<Song>) -> Unit,
    val onClearSongs: () -> Unit,
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    val onPopBackStack: () -> Unit,
    val onNavigateToMemorialPlaylist: () -> Unit,
    val onSaveSuccessNavigateHome: () -> Unit,
)

internal fun tryApplyReceiverSelectionFromSavedState(
    backStackEntry: NavBackStackEntry,
    viewModel: AfternoteEditorViewModel,
    state: AfternoteEditorState,
) {
    val id = backStackEntry.savedStateHandle[SELECTED_RECEIVER_ID_KEY] as? Long ?: return
    backStackEntry.savedStateHandle.remove<Long>(SELECTED_RECEIVER_ID_KEY)
    val receiver = viewModel.getReceiverById(id) ?: return
    state.addReceiverById(receiver.receiverId, receiver.name, receiver.relation)
}

internal fun buildEditorScreenCallbacks(
    onPopBackStack: () -> Unit,
    onNavigateToMemorialPlaylist: () -> Unit,
    editViewModel: AfternoteEditorViewModel,
    state: AfternoteEditorState,
    route: AfternoteRoute.EditorRoute,
    liveSongs: List<Song>,
    onNavigateToSelectReceiver: () -> Unit,
    onBottomNavTabSelected: (BottomNavTab) -> Unit,
): AfternoteEditorScreenCallbacks =
    AfternoteEditorScreenCallbacks(
        onBackClick = {
            onPopBackStack()
        },
        onRegisterClick = {
            state.setLeaveMessageBlocks(
                state.editorMessages.map { msg ->
                    EditorMessageTextBlock(
                        title = msg.titleState.text.toString(),
                        body = msg.contentState.text.toString(),
                    )
                },
            )
            // 폼 스냅샷은 한 번만 읽는다 — 필드마다 다시 읽으면 조립 도중 갱신이 끼어 서로 다른 시점의 값이 섞인다.
            val form = state.currentForm()
            val payload =
                SaveAfternotePayloadBuilder.build(
                    form = form,
                    accountId =
                        state.idState.text
                            .toString(),
                    password =
                        state.passwordState.text
                            .toString(),
                )
            editViewModel.saveAfternote(
                editingId = route.itemId?.toLongOrNull(),
                category = form.selectedCategory,
                payload = payload,
                selectedReceiverIds = form.afternoteEditReceivers.mapNotNull { it.id.toLongOrNull() },
                playlistSongs = liveSongs,
                memorialMedia =
                    SaveAfternoteMemorialMedia(
                        memorialVideoUrl = form.memorialVideoUrl,
                        memorialThumbnailUrl = form.memorialThumbnailUrl,
                        memorialPhotoUrl = form.memorialPhotoUrl,
                        pickedMemorialPhotoUri = form.pickedMemorialPhotoUri,
                    ),
            )
        },
        onNavigateToAddSong = onNavigateToMemorialPlaylist,
        onNavigateToSelectReceiver = onNavigateToSelectReceiver,
        onBottomNavTabSelected = onBottomNavTabSelected,
        onThumbnailBytesReady = { bytes ->
            if (bytes != null) {
                editViewModel.uploadMemorialThumbnail(bytes)
            }
        },
        onThumbnailExtractionFailed = editViewModel::onMemorialThumbnailExtractionFailed,
        onThumbnailUploadErrorConsumed = editViewModel::onThumbnailUploadErrorConsumed,
    )

@Composable
internal fun AfternoteEditorNavigation(params: AfternoteEditorNavigationParams) {
    val editViewModel = hiltViewModel<AfternoteEditorViewModel>(params.backStackEntry)
    val route = params.backStackEntry.toRoute<AfternoteRoute.EditorRoute>()
    val uiState by editViewModel.uiState.collectAsStateWithLifecycle()
    val state =
        rememberAfternoteEditorState(
            getCurrentForm = editViewModel::currentForm,
            setCategory = editViewModel::setCategory,
            setService = editViewModel::setService,
            setMemorialPhoto = editViewModel::setMemorialPhoto,
            setMemorialVideo = editViewModel::setMemorialVideo,
            addReceiver = editViewModel::addReceiver,
            addReceiverIfAbsent = editViewModel::addReceiverIfAbsent,
            applyPrefill = editViewModel::applyPrefill,
            setMemorialThumbnail = editViewModel::setMemorialThumbnail,
            setMemorialPlaylistSongs = editViewModel::setMemorialPlaylistSongs,
            deleteReceiver = editViewModel::deleteReceiver,
            replaceReceiversIfEmpty = editViewModel::replaceReceiversIfEmpty,
            setLeaveMessageBlocks = editViewModel::setLeaveMessageBlocks,
            addProcessingMethod = editViewModel::addProcessingMethod,
            deleteProcessingMethod = editViewModel::deleteProcessingMethod,
            editProcessingMethod = editViewModel::editProcessingMethod,
        )

    LaunchedEffect(Unit) {
        if (route.itemId == null) {
            params.onClearSongs()
            state.setMemorialPlaylistSongs(emptyList())
        }
    }
    LaunchedEffect(Unit) { editViewModel.refreshAuthorReceivers() }

    LaunchedEffect(uiState.authorReceivers, route.itemId) {
        if (route.itemId == null) {
            state.replaceReceiversIfEmpty(uiState.authorReceivers)
        }
    }

    LaunchedEffect(params.backStackEntry) {
        tryApplyReceiverSelectionFromSavedState(
            params.backStackEntry,
            editViewModel,
            state,
        )
    }

    LaunchedEffect(route.initialCategory, route.itemId) {
        if (route.initialCategory != null) {
            state.selectCategoryByNavKey(route.initialCategory)
        }
    }

    LaunchedEffect(uiState.pendingSaveSuccessId) {
        if (uiState.pendingSaveSuccessId != null) {
            params.onSaveSuccessNavigateHome()
            editViewModel.onSaveSuccessConsumed()
        }
    }
    val pendingThumbnailUrl = uiState.pendingThumbnailUrl
    LaunchedEffect(pendingThumbnailUrl) {
        if (pendingThumbnailUrl != null) {
            state.setMemorialThumbnail(pendingThumbnailUrl)
            editViewModel.onThumbnailUploadedConsumed()
        }
    }
    val pendingPrefill = uiState.pendingPrefill
    LaunchedEffect(pendingPrefill) {
        if (pendingPrefill != null) {
            params.onReplaceSongs(pendingPrefill.memorialPlaylistSongs)
            state.applyFormPrefill(pendingPrefill)
            editViewModel.onPrefillConsumed()
        }
    }

    val errorResult =
        remember(
            uiState.validationError,
            uiState.errorRes,
            params.liveSongs.size,
        ) { editorSaveErrorFromUiState(uiState, params.liveSongs.size) }
    val saveError: String? =
        when (errorResult) {
            is EditorSaveErrorResult.Validation -> stringResource(errorResult.messageResId)
            is EditorSaveErrorResult.Generic -> stringResource(errorResult.messageResId)
            null -> null
        }

    val callbacks =
        remember(
            params.onPopBackStack,
            params.onNavigateToMemorialPlaylist,
            params.onNavigateToSelectReceiver,
            params.onBottomNavTabSelected,
            editViewModel,
            state,
            route,
            params.liveSongs,
        ) {
            buildEditorScreenCallbacks(
                onPopBackStack = params.onPopBackStack,
                onNavigateToMemorialPlaylist = params.onNavigateToMemorialPlaylist,
                editViewModel = editViewModel,
                state = state,
                route = route,
                liveSongs = params.liveSongs,
                onNavigateToSelectReceiver = params.onNavigateToSelectReceiver,
                onBottomNavTabSelected = params.onBottomNavTabSelected,
            )
        }

    AfternoteEditorScreen(
        form = uiState.form,
        callbacks = callbacks,
        liveSongs = params.liveSongs,
        state = state,
        saveError = saveError,
        thumbnailUploadFailed = uiState.thumbnailUploadFailed,
        isPrefillLoading = uiState.isPrefillLoading,
    )
}
