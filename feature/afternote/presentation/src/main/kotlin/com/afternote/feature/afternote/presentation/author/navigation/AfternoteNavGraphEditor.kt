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
 *
 * **단일 UI 상태:** ViewModel 의 단일 [AfternoteEditorUiState] 만 collect 하고, 폼/저장진행/오류/수신자 목록은
 * 그 안의 필드로 분기한다 (CLAUDE.md *"loading/error/data 독립 스트림 분리 금지"* 규칙).
 *
 * 추모 곡 목록은 [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] StateFlow가 SSOT이며,
 * 본 화면은 [graphSongs] 스냅샷을 받아 표시하고, 변경은 [onReplaceSongs]·[onClearSongs] 인텐트로만 수행한다.
 *
 * 에디터 파사드([AfternoteEditorState])는 컴포지션 스코프에서 [rememberAfternoteEditorState]로 매번 생성되며,
 * 그래프 스코프 ViewModel에 캐싱하지 않는다. 입력값은 [androidx.compose.foundation.text.input.rememberTextFieldState]의
 * [androidx.compose.runtime.saveable.rememberSaveable] 메커니즘과 폼 SSOT(messageBlocks)로 재진입 시 복원된다.
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
    uiState.error?.let { return EditorSaveErrorResult.Raw(it) }
    uiState.errorRes?.let { return EditorSaveErrorResult.Generic(it) }
    return null
}

internal data class EditorScreenCallbacksParams(
    val onPopBackStack: () -> Unit,
    val onNavigateToMemorialPlaylist: () -> Unit,
    val editViewModel: AfternoteEditorViewModel,
    val state: AfternoteEditorState,
    val route: AfternoteRoute.EditorRoute,
    val graphSongs: List<Song>,
    val onNavigateToSelectReceiver: () -> Unit,
    val onBottomNavTabSelected: (BottomNavTab) -> Unit,
)

internal data class AfternoteEditorNavigationParams(
    val backStackEntry: NavBackStackEntry,
    val graphSongs: List<Song>,
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
    state.addReceiverFromSelection(receiver.receiverId, receiver.name, receiver.relation)
}

internal fun buildEditorScreenCallbacks(params: EditorScreenCallbacksParams): AfternoteEditorScreenCallbacks =
    AfternoteEditorScreenCallbacks(
        onBackClick = {
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
                playlistSongs = params.graphSongs,
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
    val uiState by editViewModel.uiState.collectAsStateWithLifecycle()
    val state =
        rememberAfternoteEditorState(
            getCurrentForm = editViewModel::currentForm,
            updateForm = editViewModel::updateForm,
        )

    LaunchedEffect(Unit) {
        if (route.itemId == null) {
            params.onClearSongs()
            state.resetMemorialPlaylistFormSnapshot()
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

    ObserveAsEvents(flow = editViewModel.events) { event ->
        when (event) {
            is AfternoteEditorEvent.SaveSuccess -> {
                params.onSaveSuccessNavigateHome()
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

            is AfternoteEditorEvent.PrefillLoaded -> {
                runCatching {
                    params.onReplaceSongs(event.prefill.memorialPlaylistSongs)
                    state.applyFormPrefill(event.prefill)
                }.onFailure { e ->
                    Log.e(
                        TAG_AFTERNOTE_EDIT,
                        "apply prefill failed",
                        e,
                    )
                }
            }
        }
    }

    val errorResult =
        remember(
            uiState.validationError,
            uiState.error,
            uiState.errorRes,
            params.graphSongs.size,
        ) { editorSaveErrorFromUiState(uiState, params.graphSongs.size) }
    val saveError =
        when (errorResult) {
            is EditorSaveErrorResult.Validation -> AfternoteEditorSaveError(stringResource(errorResult.messageResId))
            is EditorSaveErrorResult.Raw -> AfternoteEditorSaveError(errorResult.message)
            is EditorSaveErrorResult.Generic -> AfternoteEditorSaveError(stringResource(errorResult.messageResId))
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
            params.graphSongs,
        ) {
            buildEditorScreenCallbacks(
                EditorScreenCallbacksParams(
                    onPopBackStack = params.onPopBackStack,
                    onNavigateToMemorialPlaylist = params.onNavigateToMemorialPlaylist,
                    editViewModel = editViewModel,
                    state = state,
                    route = route,
                    graphSongs = params.graphSongs,
                    onNavigateToSelectReceiver = params.onNavigateToSelectReceiver,
                    onBottomNavTabSelected = params.onBottomNavTabSelected,
                ),
            )
        }

    AfternoteEditorScreen(
        form = uiState.form,
        callbacks = callbacks,
        graphSongs = params.graphSongs,
        state = state,
        saveError = saveError,
    )
}
