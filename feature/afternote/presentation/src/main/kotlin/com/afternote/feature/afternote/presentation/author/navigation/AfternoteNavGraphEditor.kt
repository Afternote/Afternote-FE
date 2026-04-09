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
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorEvent
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorSaveError
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreen
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreenCallbacks
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorUiEvent
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.RegisterAfternotePayloadBuilder
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteSaveState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.author.navigation.model.SELECTED_RECEIVER_ID_KEY

/**
 * 작성자 에디터 플로우: type-safe [AfternoteRoute.EditorRoute] + 단방향 이벤트.
 *
 * **데이터 SSOT:** 편집 본문은 [com.afternote.feature.afternote.domain.repository.AfternoteRepository]가 담당한다.
 * 홈의 `visibleItems` 스냅샷은 에디터에 전달하지 않는다. 식별은 라우트의 `itemId`·`initialCategory` 정도로 최소화한다.
 *
 * **LoadForEdit 트리거가 Compose에 있는 이유:** [MemorialPlaylistStateHolder]는
 * [com.afternote.feature.afternote.presentation.AfternoteHostViewModel]에 묶인 세션 초안이라,
 * VM 생성자 주입만으로는 함께 쥐기 어렵다. 실제 원본 로드는 여전히 ViewModel → Repository 경로다.
 */
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
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val onNavigateToSelectReceiver: () -> Unit,
    val onBottomNavTabSelected: (BottomNavTab) -> Unit,
)

internal data class AfternoteEditorNavigationParams(
    val backStackEntry: NavBackStackEntry,
    val navController: NavController,
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val editState: AfternoteEditorState?,
    val onEditStateChanged: (AfternoteEditorState?) -> Unit,
    val onEditStateClear: () -> Unit,
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
        onRegisterClick = {
            val payload = RegisterAfternotePayloadBuilder.fromEditorState(params.state)
            params.editViewModel.onEvent(
                AfternoteEditorUiEvent.Save(
                    editingId = params.route.itemId?.toLongOrNull(),
                    editorCategory = params.state.selectedCategory,
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
internal fun AfternoteEditorNavigation(params: AfternoteEditorNavigationParams) {
    val editViewModel = hiltViewModel<AfternoteEditorViewModel>(params.backStackEntry)
    val route = params.backStackEntry.toRoute<AfternoteRoute.EditorRoute>()
    val saveState by editViewModel.saveState.collectAsStateWithLifecycle()
    val authorReceivers by editViewModel.authorReceiversUi.collectAsStateWithLifecycle()
    val state = params.editState ?: editViewModel.editorFormState

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

    LaunchedEffect(authorReceivers, route.itemId) {
        if (route.itemId == null) {
            state.replaceReceiversIfEmpty(authorReceivers)
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

    // 수정 진입: 라우트 ID만 사용 → VM이 Repository(getDetail)로 SSOT 로드. holder는 그래프 스코프 초안 전달용.
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
                    playlistStateHolder = params.playlistStateHolder,
                    onNavigateToSelectReceiver = params.onNavigateToSelectReceiver,
                    onBottomNavTabSelected = params.onBottomNavTabSelected,
                ),
            ),
        playlistStateHolder = params.playlistStateHolder,
        initialListItem = null,
        state = state,
        saveError = saveError,
    )
}
