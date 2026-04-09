package com.afternote.feature.afternote.presentation.author.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorEvent
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorSaveError
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreen
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreenCallbacks
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorUiEvent
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteSaveState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.state.rememberAfternoteEditorState
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.author.navigation.model.SELECTED_RECEIVER_ID_KEY

/**
 * 에디터에서 사용할 아이템 목록을 결정.
 * 홈에서 공유된 visibleItems가 비어 있으면 빈 리스트를 반환합니다.
 * (향후 API 기본 아이템은 ViewModel에서 직접 로딩)
 */
internal fun resolveListItems(afternoteVisibleItems: List<ListItem>): List<ListItem> = afternoteVisibleItems

internal sealed class EditSaveErrorResult {
    data class Validation(
        val messageResId: Int,
    ) : EditSaveErrorResult()

    data class Raw(
        val message: String,
    ) : EditSaveErrorResult()
}

internal fun editSaveErrorFromState(
    saveState: AfternoteSaveState,
    playlistSongCount: Int,
): EditSaveErrorResult? {
    if (saveState.validationError == AfternoteValidationError.PLAYLIST_SONGS_REQUIRED &&
        playlistSongCount > 0
    ) {
        return null
    }
    saveState.validationError?.let { return EditSaveErrorResult.Validation(it.messageResId) }
    saveState.error?.let { return EditSaveErrorResult.Raw(it) }
    return null
}

internal data class EditScreenCallbacksParams(
    val navController: NavController,
    val editViewModel: AfternoteEditorViewModel,
    val editState: AfternoteEditorState?,
    val onEditStateChanged: (AfternoteEditorState?) -> Unit,
    val onEditStateClear: () -> Unit,
    val state: AfternoteEditorState,
    val route: AfternoteRoute.EditorRoute,
    val initialListItem: ListItem?,
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val onNavigateToSelectReceiver: () -> Unit,
    val onBottomNavTabSelected: (BottomNavTab) -> Unit,
)

internal data class AfternoteEditorNavigationParams(
    val backStackEntry: NavBackStackEntry,
    val navController: NavController,
    val afternoteVisibleItems: List<ListItem>,
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val editState: AfternoteEditorState?,
    val onEditStateChanged: (AfternoteEditorState?) -> Unit,
    val onEditStateClear: () -> Unit,
    val onRequestHomeRefresh: () -> Unit = {},
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
)

internal fun navigateToAfternoteHomeOnSaveSuccess(
    onEditStateClear: () -> Unit,
    navController: NavController,
) {
    onEditStateClear()
    navController.navigate(AfternoteRoute.AfternoteHomeRoute) {
        popUpTo(AfternoteRoute.AfternoteHomeRoute) { inclusive = true }
        launchSingleTop = true
    }
}

internal fun tryApplyReceiverSelectionFromSavedState(
    backStackEntry: NavBackStackEntry,
    viewModel: AfternoteEditorViewModel,
    state: AfternoteEditorState,
) {
    val id = backStackEntry.savedStateHandle[SELECTED_RECEIVER_ID_KEY] as? Long ?: return
    backStackEntry.savedStateHandle.remove<Long>(SELECTED_RECEIVER_ID_KEY)
    val receiver = viewModel.getReceiverById(id) ?: return
    state.addReceiverFromSelection(receiver.receiverId, receiver.name, receiver.relation)
}

internal fun buildEditScreenCallbacks(params: EditScreenCallbacksParams): AfternoteEditorScreenCallbacks =
    AfternoteEditorScreenCallbacks(
        onBackClick = {
            params.onEditStateClear()
            params.navController.popBackStack()
        },
        onRegisterClick = { payload: RegisterAfternotePayload ->
            params.editViewModel.onEvent(
                AfternoteEditorUiEvent.Save(
                    editingId =
                        params.route.itemId?.toLongOrNull()
                            ?: params.initialListItem?.id?.toLongOrNull(),
                    editorCategory = EditorCategory.fromDisplayLabel(params.state.selectedCategory),
                    payload = payload,
                    selectedReceiverIds = params.state.afternoteEditReceivers.mapNotNull { it.id.toLongOrNull() },
                    playlistStateHolder = params.playlistStateHolder,
                    memorialMedia =
                        SaveAfternoteMemorialMedia(
                            funeralVideoUrl = params.state.funeralVideoUrl,
                            funeralThumbnailUrl = params.state.funeralThumbnailUrl,
                            memorialPhotoUrl = params.state.memorialPhotoUrl,
                            pickedMemorialPhotoUri = params.state.pickedMemorialPhotoUri,
                        ),
                ),
            )
        },
        onNavigateToAddSong = { params.navController.navigate(AfternoteRoute.MemorialPlaylistRoute) },
        onNavigateToSelectReceiver = params.onNavigateToSelectReceiver,
        onBottomNavTabSelected = params.onBottomNavTabSelected,
        onThumbnailBytesReady = { bytes ->
            if (bytes != null) {
                params.editViewModel.onEvent(
                    AfternoteEditorUiEvent.UploadThumbnail(
                        bytes,
                    ),
                )
            }
        },
    )

@Composable
internal fun AfternoteEditorNavigation(
    params: AfternoteEditorNavigationParams,
    editViewModel: AfternoteEditorViewModel = hiltViewModel(),
) {
    val route = params.backStackEntry.toRoute<AfternoteRoute.EditorRoute>()
    val visibleItems =
        remember(params.afternoteVisibleItems) {
            resolveListItems(params.afternoteVisibleItems)
        }
    val initialItem =
        remember(route.itemId, visibleItems) {
            route.itemId?.let { id -> visibleItems.find { it.id == id } }
        }
    if (route.itemId != null && initialItem == null) {
        Log.w(
            TAG_AFTERNOTE_EDIT,
            "Edit opened but item not found: itemId=${route.itemId}, " +
                "listSize=${visibleItems.size}",
        )
    }
    val saveState by editViewModel.saveState.collectAsStateWithLifecycle()
    val directoryReceivers by editViewModel.authorDirectoryReceiversUi.collectAsStateWithLifecycle()
    val newState = rememberAfternoteEditorState()
    val state = params.editState ?: newState

    // 새 글 작성 시 기존 상태 초기화 (목적지 화면이 스스로 책임)
    LaunchedEffect(Unit) {
        if (route.itemId == null) {
            params.onEditStateClear()
            params.playlistStateHolder.clearAllSongs()
        }
    }
    LaunchedEffect(Unit) {
        if (params.editState == null) {
            params.onEditStateChanged(state)
        }
    }
    LaunchedEffect(Unit) { editViewModel.onEvent(AfternoteEditorUiEvent.LoadReceivers) }

    LaunchedEffect(directoryReceivers, route.itemId) {
        if (route.itemId == null) {
            state.replaceReceiversIfEmpty(directoryReceivers)
        }
    }

    val isEditCurrentDestination =
        params.navController.currentBackStackEntry == params.backStackEntry
    LaunchedEffect(isEditCurrentDestination) {
        if (isEditCurrentDestination) {
            tryApplyReceiverSelectionFromSavedState(
                params.backStackEntry,
                editViewModel,
                state,
            )
        }
    }

    LaunchedEffect(route.initialCategory, route.itemId) {
        if (route.itemId == null && route.initialCategory != null) {
            state.onCategorySelected(route.initialCategory)
        }
    }

    LaunchedEffect(route.itemId) {
        val id = route.itemId?.toLongOrNull() ?: return@LaunchedEffect
        if (state.loadedItemId != route.itemId) {
            editViewModel.onEvent(
                AfternoteEditorUiEvent.LoadForEdit(
                    afternoteId = id,
                    state = state,
                    playlistStateHolder = params.playlistStateHolder,
                ),
            )
        }
    }

    LaunchedEffect(Unit) {
        editViewModel.events.collect { event ->
            when (event) {
                is AfternoteEditorEvent.SaveSuccess -> {
                    params.onRequestHomeRefresh()
                    navigateToAfternoteHomeOnSaveSuccess(
                        params.onEditStateClear,
                        params.navController,
                    )
                }

                is AfternoteEditorEvent.ThumbnailUploaded -> {
                    runCatching { state.onFuneralThumbnailDataUrlReady(event.url) }
                        .onFailure { e ->
                            Log.e(
                                TAG_AFTERNOTE_EDIT,
                                "apply thumbnailUrl failed",
                                e,
                            )
                        }
                }
            }
        }
    }

    val errorResult =
        remember(
            saveState.validationError,
            saveState.error,
            params.playlistStateHolder.songs.size,
        ) { editSaveErrorFromState(saveState, params.playlistStateHolder.songs.size) }
    val saveError =
        when (errorResult) {
            is EditSaveErrorResult.Validation -> AfternoteEditorSaveError(stringResource(errorResult.messageResId))
            is EditSaveErrorResult.Raw -> AfternoteEditorSaveError(errorResult.message)
            null -> null
        }

    AfternoteEditorScreen(
        callbacks =
            buildEditScreenCallbacks(
                EditScreenCallbacksParams(
                    navController = params.navController,
                    editViewModel = editViewModel,
                    editState = params.editState,
                    onEditStateChanged = params.onEditStateChanged,
                    onEditStateClear = params.onEditStateClear,
                    state = state,
                    route = route,
                    initialListItem = initialItem,
                    playlistStateHolder = params.playlistStateHolder,
                    onNavigateToSelectReceiver = params.onNavigateToSelectReceiver,
                    onBottomNavTabSelected = params.onBottomNavTabSelected,
                ),
            ),
        playlistStateHolder = params.playlistStateHolder,
        initialListItem = if (route.itemId != null) null else initialItem,
        state = state,
        saveError = saveError,
    )
}
