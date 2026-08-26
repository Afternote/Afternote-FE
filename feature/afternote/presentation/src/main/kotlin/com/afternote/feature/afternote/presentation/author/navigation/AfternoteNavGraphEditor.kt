package com.afternote.feature.afternote.presentation.author.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorBody
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorScreen
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternotePayloadBuilder
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorUiState
import com.afternote.feature.afternote.presentation.author.editor.state.rememberAfternoteEditorState
import com.afternote.feature.afternote.presentation.author.navigation.model.SELECTED_RECEIVER_ID_KEY

@StringRes
internal fun editorSaveErrorMessageRes(uiState: AfternoteEditorUiState): Int? = uiState.validationError?.messageResId ?: uiState.errorRes

internal fun tryApplyReceiverSelectionFromSavedState(
    backStackEntry: NavBackStackEntry,
    viewModel: AfternoteEditorViewModel,
    state: AfternoteEditorState,
) {
    val id = backStackEntry.savedStateHandle[SELECTED_RECEIVER_ID_KEY] as? Long ?: return
    backStackEntry.savedStateHandle.remove<Long>(SELECTED_RECEIVER_ID_KEY)
    val receiver = viewModel.getReceiverById(id) ?: return
    state.addReceiverById(id, receiver.name, receiver.label)
}

internal fun buildOnRegisterClick(
    editViewModel: AfternoteEditorViewModel,
    state: AfternoteEditorState,
): () -> Unit =
    {
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
            payload = payload,
            selectedReceiverIds = form.afternoteEditReceivers.mapNotNull { it.id.toLongOrNull() },
            memorialMedia =
                SaveAfternoteMemorialMedia(
                    memorialVideoUrl = form.memorialVideoUrl,
                    memorialThumbnailUrl = form.memorialThumbnailUrl,
                    memorialPhotoUrl = form.memorialPhotoUrl,
                    pickedMemorialPhotoUri = form.pickedMemorialPhotoUri,
                ),
        )
    }

/**
 * 작성자 에디터 화면: type-safe editor flow + 단방향 이벤트.
 *
 * 홈의 `visibleItems` 스냅샷은 에디터에 전달하지 않는다. 식별은 라우트의 `itemId`·`initialType` 정도로 최소화한다.
 *
 * **수정 진입 데이터 로드:** 상세 화면과 같이 [AfternoteEditorViewModel]의 `init`에서
 * [androidx.lifecycle.SavedStateHandle]의 `itemId`만 보고 Repository `getDetail`을 호출한다 (Compose `LaunchedEffect` 위임 없음).
 */
@Composable
internal fun AfternoteEditorNavigation(
    backStackEntry: NavBackStackEntry,
    liveSongs: List<Song>,
    onReplaceSongs: (List<Song>) -> Unit,
    onClearSongs: () -> Unit,
    onNavigateToMemorialPlaylist: () -> Unit,
    onNavigateToSelectReceiver: () -> Unit,
    onPopBackStack: () -> Unit,
    onSaveSuccessNavigateHome: () -> Unit,
) {
    val editViewModel = hiltViewModel<AfternoteEditorViewModel>(backStackEntry)
    val uiState by editViewModel.uiState.collectAsStateWithLifecycle()
    val state =
        rememberAfternoteEditorState(
            getCurrentForm = editViewModel::currentForm,
            setType = editViewModel::setType,
            setService = editViewModel::setService,
            setMemorialPhoto = editViewModel::setMemorialPhoto,
            setMemorialVideo = editViewModel::setMemorialVideo,
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
        if (!editViewModel.isEditing) {
            onClearSongs()
            state.setMemorialPlaylistSongs(emptyList())
        }
        editViewModel.refreshAuthorReceivers()
    }

    LaunchedEffect(liveSongs) {
        state.setMemorialPlaylistSongs(liveSongs)
    }

    LaunchedEffect(uiState.authorReceivers, editViewModel.isEditing) {
        if (!editViewModel.isEditing) {
            state.replaceReceiversIfEmpty(uiState.authorReceivers)
        }
    }

    LaunchedEffect(backStackEntry) {
        tryApplyReceiverSelectionFromSavedState(
            backStackEntry,
            editViewModel,
            state,
        )
    }

    LaunchedEffect(uiState.pendingSaveSuccessId) {
        if (uiState.pendingSaveSuccessId != null) {
            onSaveSuccessNavigateHome()
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
            onReplaceSongs(
                (pendingPrefill.content as? EditorContentPrefill.Memorial)?.playlistSongs.orEmpty(),
            )
            state.applyFormPrefill(pendingPrefill)
            editViewModel.onPrefillConsumed()
        }
    }

    val saveError = editorSaveErrorMessageRes(uiState)?.let { stringResource(it) }

    val onRegisterClick =
        remember(editViewModel, state) {
            buildOnRegisterClick(
                editViewModel = editViewModel,
                state = state,
            )
        }
    AfternoteEditorScreen(
        form = uiState.form,
        onBackClick = onPopBackStack,
        onRegisterClick = onRegisterClick,
        onThumbnailUploadErrorConsumed = editViewModel::onThumbnailUploadErrorConsumed,
        onValidationErrorConsumed = editViewModel::onValidationErrorConsumed,
        content = { snackbarHostState ->
            AfternoteEditorBody(
                state = state,
                form = uiState.form,
                onNavigateToMemorialPlaylist = onNavigateToMemorialPlaylist,
                onNavigateToSelectReceiver = onNavigateToSelectReceiver,
                onThumbnailBytesReady = editViewModel::uploadMemorialThumbnail,
                onThumbnailExtractionFailed = editViewModel::onMemorialThumbnailExtractionFailed,
                onCaptureFailed = editViewModel::onMemorialCaptureLaunchFailed,
                snackbarHostState = snackbarHostState,
                isPrefillLoading = uiState.isPrefillLoading,
            )
        },
        state = state,
        saveError = saveError,
        thumbnailUploadFailed = uiState.thumbnailUploadFailed,
        isPrefillLoading = uiState.isPrefillLoading,
    )
}
