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
import androidx.navigation.toRoute
import com.afternote.core.ui.ObserveAsEvents
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorEvent
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorSaveError
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreen
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreenCallbacks
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternotePayloadBuilder
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteSaveState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.author.navigation.model.SELECTED_RECEIVER_ID_KEY

/**
 * 작성자 에디터 플로우: type-safe [AfternoteRoute.EditorRoute] + 단방향 이벤트.
 *
 * **데이터 SSOT:** 편집 본문은 [com.afternote.feature.afternote.domain.repository.AfternoteRepository]가 담당한다.
 * 홈의 `visibleItems` 스냅샷은 에디터에 전달하지 않는다. 식별은 라우트의 `itemId`·`initialCategory` 정도로 최소화한다.
 *
 * **수정 진입 데이터 로드:** 상세 화면과 같이 [com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel]의 `init`에서
 * [androidx.lifecycle.SavedStateHandle]의 `itemId`만 보고 Repository `getDetail`을 호출한다 (Compose `LaunchedEffect` 위임 없음).
 * [MemorialPlaylistStateHolder]는 그래프 스코프 런타임 버퍼이며, 곡 목록 복원 SSOT는 폼·스냅샷의 `memorialPlaylistSongs`이다.
 * 서브화면에서 복귀 시 [com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState.syncMemorialPlaylistFromGraphHolderIfAttached]로 홀더→폼을 맞춘다.
 *
 * ViewModel 단발 이벤트는 [com.afternote.core.ui.ObserveAsEvents]로만 수집한다 (백그라운드에서 네비게이션 부수 효과 방지).
 */
internal sealed class EditorSaveErrorResult {
    data class Validation(
        val messageResId: Int,
    ) : EditorSaveErrorResult()

    data class Raw(
        val message: String,
    ) : EditorSaveErrorResult()
}

internal fun editorSaveErrorFromState(
    saveState: AfternoteSaveState,
    playlistSongCount: Int,
): EditorSaveErrorResult? {
    if (saveState.validationError == AfternoteValidationError.PLAYLIST_SONGS_REQUIRED &&
        playlistSongCount > 0
    ) {
        return null
    }
    saveState.validationError?.let { return EditorSaveErrorResult.Validation(it.messageResId) }
    saveState.error?.let { return EditorSaveErrorResult.Raw(it) }
    return null
}

internal data class EditorScreenCallbacksParams(
    val onPopBackStack: () -> Unit,
    val onNavigateToMemorialPlaylist: () -> Unit,
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
    val playlistStateHolder: MemorialPlaylistStateHolder,
    val editState: AfternoteEditorState?,
    val onEditStateChanged: (AfternoteEditorState?) -> Unit,
    val onEditStateClear: () -> Unit,
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    val isEditorRouteCurrent: Boolean,
    val onPopBackStack: () -> Unit,
    val onNavigateToMemorialPlaylist: () -> Unit,
    val onSaveSuccessNavigateHome: () -> Unit,
)

internal fun navigateToAfternoteHomeOnSaveSuccess(
    onEditStateClear: () -> Unit,
    onSaveSuccessNavigateHome: () -> Unit,
) {
    onEditStateClear()
    onSaveSuccessNavigateHome()
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

internal fun buildEditorScreenCallbacks(params: EditorScreenCallbacksParams): AfternoteEditorScreenCallbacks =
    AfternoteEditorScreenCallbacks(
        onBackClick = {
            params.onEditStateClear()
            params.onPopBackStack()
        },
        onRegisterClick = {
            params.state.persistEditorMessagesFromTyping(
                params.state.editorMessages.map { msg ->
                    EditorMessageTextBlock(
                        title = msg.titleState.text.toString(),
                        body = msg.contentState.text.toString(),
                    )
                },
            )
            val payload = SaveAfternotePayloadBuilder.fromEditorState(params.state)
            params.editViewModel.saveAfternote(
                editingId = params.route.itemId?.toLongOrNull(),
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
        onNavigateToAddSong = params.onNavigateToMemorialPlaylist,
        onNavigateToSelectReceiver = params.onNavigateToSelectReceiver,
        onBottomNavTabSelected = params.onBottomNavTabSelected,
        onThumbnailBytesReady = { bytes ->
            if (bytes != null) {
                params.editViewModel.uploadMemorialThumbnail(bytes)
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
            state.resetMemorialPlaylistFormSnapshot()
        }
    }
    LaunchedEffect(Unit) {
        if (params.editState == null) {
            params.onEditStateChanged(state)
        }
    }
    LaunchedEffect(Unit) { editViewModel.refreshAuthorReceivers() }

    LaunchedEffect(authorReceivers, route.itemId) {
        if (route.itemId == null) {
            state.replaceReceiversIfEmpty(authorReceivers)
        }
    }

    LaunchedEffect(params.isEditorRouteCurrent) {
        if (params.isEditorRouteCurrent) {
            tryApplyReceiverSelectionFromSavedState(
                params.backStackEntry,
                editViewModel,
                state,
            )
            state.syncMemorialPlaylistFromGraphHolderIfAttached()
        }
    }

    LaunchedEffect(route.initialCategory, route.itemId) {
        if (route.itemId == null && route.initialCategory != null) {
            state.selectCategoryByNavKey(route.initialCategory)
        }
    }

    ObserveAsEvents(flow = editViewModel.events) { event ->
        when (event) {
            is AfternoteEditorEvent.SaveSuccess -> {
                navigateToAfternoteHomeOnSaveSuccess(
                    params.onEditStateClear,
                    params.onSaveSuccessNavigateHome,
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

    val errorResult =
        remember(
            saveState.validationError,
            saveState.error,
            params.playlistStateHolder.songs.size,
        ) { editorSaveErrorFromState(saveState, params.playlistStateHolder.songs.size) }
    val saveError =
        when (errorResult) {
            is EditorSaveErrorResult.Validation -> AfternoteEditorSaveError(stringResource(errorResult.messageResId))
            is EditorSaveErrorResult.Raw -> AfternoteEditorSaveError(errorResult.message)
            null -> null
        }

    val callbacks =
        remember(
            params.onPopBackStack,
            params.onNavigateToMemorialPlaylist,
            params.onNavigateToSelectReceiver,
            params.onBottomNavTabSelected,
            params.onEditStateClear,
            params.onEditStateChanged,
            params.editState,
            editViewModel,
            state,
            route,
            params.playlistStateHolder,
        ) {
            buildEditorScreenCallbacks(
                EditorScreenCallbacksParams(
                    onPopBackStack = params.onPopBackStack,
                    onNavigateToMemorialPlaylist = params.onNavigateToMemorialPlaylist,
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
            )
        }

    AfternoteEditorScreen(
        callbacks = callbacks,
        playlistStateHolder = params.playlistStateHolder,
        initialListItem = null,
        state = state,
        saveError = saveError,
    )
}
