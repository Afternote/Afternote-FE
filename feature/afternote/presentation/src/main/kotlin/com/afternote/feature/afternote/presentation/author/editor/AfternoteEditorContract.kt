package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.MemorialPlaylistStateHolder

/**
 * 콜백 그룹 (S107: 파라미터 7개 이하 유지).
 */
data class AfternoteEditorScreenCallbacks(
    val onBackClick: () -> Unit = {},
    val onRegisterClick: (RegisterAfternotePayload) -> Unit = {},
    val onNavigateToAddSong: () -> Unit = {},
    val onNavigateToSelectReceiver: () -> Unit = {},
    val onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    val onThumbnailBytesReady: (ByteArray?) -> Unit = {},
)

/**
 * Message to show when save fails (validation or API error).
 * When non-null, the screen shows a Snackbar with this text.
 */
data class AfternoteEditorSaveError(
    val message: String,
)

/** 단발성 이벤트. ViewModel [kotlinx.coroutines.channels.Channel]로 한 번만 소비됩니다. */
sealed interface AfternoteEditorEvent {
    data class SaveSuccess(
        val savedId: Long,
    ) : AfternoteEditorEvent

    data class ThumbnailUploaded(
        val url: String,
    ) : AfternoteEditorEvent
}

/** UI → ViewModel 사용자 액션. */
sealed interface AfternoteEditorUiEvent {
    data object LoadReceivers : AfternoteEditorUiEvent

    data class UploadThumbnail(
        val jpegBytes: ByteArray,
    ) : AfternoteEditorUiEvent

    data class Save(
        val editingId: Long?,
        val editorCategory: EditorCategory,
        val payload: RegisterAfternotePayload,
        val selectedReceiverIds: List<Long>,
        val playlistStateHolder: MemorialPlaylistStateHolder?,
        val memorialMedia: SaveAfternoteMemorialMedia,
    ) : AfternoteEditorUiEvent

    data class LoadForEdit(
        val afternoteId: Long,
        val state: AfternoteEditorState,
        val playlistStateHolder: MemorialPlaylistStateHolder?,
    ) : AfternoteEditorUiEvent
}

/** 저장 시 추모 미디어 필드 (로컬 URI / 기존 URL 혼재). */
data class SaveAfternoteMemorialMedia(
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val pickedMemorialPhotoUri: String? = null,
)
