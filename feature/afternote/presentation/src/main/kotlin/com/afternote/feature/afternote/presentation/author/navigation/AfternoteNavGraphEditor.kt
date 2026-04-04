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
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorEvent
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorSaveError
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreen
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreenCallbacks
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteSaveState
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.model.rememberAfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.author.navigation.model.SELECTED_RECEIVER_ID_KEY

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
    val editStateHandling: AfternoteEditorStateHandling,
    val state: AfternoteEditorState,
    val route: AfternoteRoute.EditRoute,
    val initialItem: Item?,
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val onNavigateToSelectReceiver: () -> Unit,
    val onBottomNavTabSelected: (BottomNavTab) -> Unit,
)

internal data class AfternoteEditorDestinationParams(
    val backStackEntry: NavBackStackEntry,
    val navController: NavController,
    val afternoteItems: List<Item>,
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val afternoteProvider: AfternoteEditorDataProvider,
    val editStateHandling: AfternoteEditorStateHandling,
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
)

internal fun navigateToAfternoteHomeOnSaveSuccess(
    editStateHandling: AfternoteEditorStateHandling,
    navController: NavController,
) {
    editStateHandling.onClear()
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
            params.editStateHandling.onClear()
            params.navController.popBackStack()
        },
        onRegisterClick = { payload: RegisterAfternotePayload ->
            params.editViewModel.saveAfternote(
                editingId =
                    params.route.itemId?.toLongOrNull()
                        ?: params.initialItem?.id?.toLongOrNull(),
                category = params.state.selectedCategory,
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
            )
        },
        onNavigateToAddSong = { params.navController.navigate(AfternoteRoute.MemorialPlaylistRoute) },
        onNavigateToSelectReceiver = params.onNavigateToSelectReceiver,
        onBottomNavTabSelected = params.onBottomNavTabSelected,
        onThumbnailBytesReady = { bytes ->
            if (bytes != null) params.editViewModel.uploadMemorialThumbnail(bytes)
        },
    )

@Composable
internal fun AfternoteEditorDestination(
    params: AfternoteEditorDestinationParams,
    editViewModel: AfternoteEditorViewModel = hiltViewModel(),
) {
    val route = params.backStackEntry.toRoute<AfternoteRoute.EditRoute>()
    val listItems =
        remember(params.afternoteItems, params.afternoteProvider) {
            resolveListItems(params.afternoteItems, params.afternoteProvider)
        }
    val initialItem =
        remember(route.itemId, listItems) {
            route.itemId?.let { id -> listItems.find { it.id == id } }
        }
    if (route.itemId != null && initialItem == null) {
        Log.w(
            TAG_AFTERNOTE_EDIT,
            "Edit opened but item not found: itemId=${route.itemId}, " +
                "listSize=${listItems.size}",
        )
    }
    val saveState by editViewModel.saveState.collectAsStateWithLifecycle()
    val newState = rememberAfternoteEditorState()
    val existingState = params.editStateHandling.state
    val state = existingState ?: newState

    // 새 글 작성 시 기존 상태 초기화 (목적지 화면이 스스로 책임)
    LaunchedEffect(Unit) {
        if (route.itemId == null) {
            params.editStateHandling.onClear()
            params.playlistStateHolder.clearAllSongs()
        }
    }
    LaunchedEffect(Unit) {
        if (params.editStateHandling.state == null) {
            params.editStateHandling.onStateChanged(state)
        }
    }
    LaunchedEffect(Unit) { editViewModel.loadReceivers() }

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
            editViewModel.loadForEdit(
                afternoteId = id,
                state = state,
                playlistStateHolder = params.playlistStateHolder,
            )
        }
    }

    LaunchedEffect(Unit) {
        editViewModel.events.collect { event ->
            when (event) {
                is AfternoteEditorEvent.SaveSuccess -> {
                    navigateToAfternoteHomeOnSaveSuccess(
                        params.editStateHandling,
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
                    editStateHandling = params.editStateHandling,
                    state = state,
                    route = route,
                    initialItem = initialItem,
                    playlistStateHolder = params.playlistStateHolder,
                    onNavigateToSelectReceiver = params.onNavigateToSelectReceiver,
                    onBottomNavTabSelected = params.onBottomNavTabSelected,
                ),
            ),
        playlistStateHolder = params.playlistStateHolder,
        initialItem = if (route.itemId != null) null else initialItem,
        state = state,
        saveError = saveError,
    )
}
