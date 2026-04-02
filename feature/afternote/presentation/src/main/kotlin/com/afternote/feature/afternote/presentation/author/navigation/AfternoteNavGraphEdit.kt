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
import com.afternote.feature.afternote.presentation.author.edit.AfternoteEditSaveError
import com.afternote.feature.afternote.presentation.author.edit.AfternoteEditScreen
import com.afternote.feature.afternote.presentation.author.edit.AfternoteEditScreenCallbacks
import com.afternote.feature.afternote.presentation.author.edit.AfternoteEditViewModel
import com.afternote.feature.afternote.presentation.author.edit.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteSaveState
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.edit.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.edit.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.edit.model.rememberAfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.provider.AfternoteEditDataProvider
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
    val editViewModel: AfternoteEditViewModel,
    val editStateHandling: AfternoteEditStateHandling,
    val state: AfternoteEditState,
    val route: AfternoteRoute.EditRoute,
    val initialItem: Item?,
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val onNavigateToSelectReceiver: () -> Unit,
    val onBottomNavTabSelected: (BottomNavTab) -> Unit,
)

internal data class AfternoteEditDestinationParams(
    val backStackEntry: NavBackStackEntry,
    val navController: NavController,
    val afternoteItems: List<Item>,
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val afternoteProvider: AfternoteEditDataProvider,
    val editStateHandling: AfternoteEditStateHandling,
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
)

internal fun navigateToAfternoteListOnSaveSuccess(
    editStateHandling: AfternoteEditStateHandling,
    navController: NavController,
) {
    editStateHandling.onClear()
    navController.navigate(AfternoteRoute.AfternoteListRoute) {
        popUpTo(AfternoteRoute.AfternoteListRoute) { inclusive = true }
        launchSingleTop = true
    }
}

internal fun applyUploadedThumbnailAndClear(
    url: String,
    state: AfternoteEditState,
    viewModel: AfternoteEditViewModel,
) {
    runCatching { state.onFuneralThumbnailDataUrlReady(url) }
        .onFailure { e -> Log.e(TAG_AFTERNOTE_EDIT, "apply uploadedThumbnailUrl failed", e) }
    viewModel.clearUploadedThumbnailUrl()
}

internal fun tryApplyReceiverSelectionFromSavedState(
    backStackEntry: NavBackStackEntry,
    viewModel: AfternoteEditViewModel,
    state: AfternoteEditState,
) {
    val id = backStackEntry.savedStateHandle[SELECTED_RECEIVER_ID_KEY] as? Long ?: return
    backStackEntry.savedStateHandle.remove<Long>(SELECTED_RECEIVER_ID_KEY)
    val receiver = viewModel.getReceiverById(id) ?: return
    state.addReceiverFromSelection(receiver.receiverId, receiver.name, receiver.relation)
}

internal fun buildEditScreenCallbacks(params: EditScreenCallbacksParams): AfternoteEditScreenCallbacks =
    AfternoteEditScreenCallbacks(
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
internal fun AfternoteEditDestination(
    params: AfternoteEditDestinationParams,
    editViewModel: AfternoteEditViewModel = hiltViewModel(),
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
    val newState = rememberAfternoteEditState()
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

    LaunchedEffect(saveState.saveSuccess) {
        if (saveState.saveSuccess) {
            navigateToAfternoteListOnSaveSuccess(params.editStateHandling, params.navController)
        }
    }

    val uploadedThumbnailUrl by editViewModel.uploadedThumbnailUrl.collectAsStateWithLifecycle(
        initialValue = null,
    )
    LaunchedEffect(uploadedThumbnailUrl) {
        uploadedThumbnailUrl?.let { url ->
            applyUploadedThumbnailAndClear(url, state, editViewModel)
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
            is EditSaveErrorResult.Validation -> AfternoteEditSaveError(stringResource(errorResult.messageResId))
            is EditSaveErrorResult.Raw -> AfternoteEditSaveError(errorResult.message)
            null -> null
        }

    AfternoteEditScreen(
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
