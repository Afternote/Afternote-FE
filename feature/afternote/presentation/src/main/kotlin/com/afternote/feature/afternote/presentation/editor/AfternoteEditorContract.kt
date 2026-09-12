package com.afternote.feature.afternote.presentation.editor

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorErrorEvent
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState

/** 편집기 공유 흐름이 ViewModel에 보내는 변경·소비 의도. */
internal sealed interface AfternoteEditorIntent : MviIntent {
    data class SetType(
        val type: AfternoteType,
    ) : AfternoteEditorIntent

    data class SetService(
        val service: String,
    ) : AfternoteEditorIntent

    data class SetMemorialPhoto(
        val uri: String,
    ) : AfternoteEditorIntent

    data object RemoveMemorialPhoto : AfternoteEditorIntent

    data class SetMemorialVideo(
        val url: String,
    ) : AfternoteEditorIntent

    data object RemoveMemorialVideo : AfternoteEditorIntent

    data class SetMemorialThumbnail(
        val dataUrl: String,
    ) : AfternoteEditorIntent

    data class AddMemorialPlaylistSongs(
        val songs: List<Song>,
    ) : AfternoteEditorIntent

    data class RemoveMemorialPlaylistSongs(
        val selectionKeys: Set<String>,
    ) : AfternoteEditorIntent

    data object ClearMemorialPlaylistSongs : AfternoteEditorIntent

    data class DeleteReceiver(
        val receiverId: Long,
    ) : AfternoteEditorIntent

    data class AddReceiverIfAbsent(
        val receiverId: Long,
        val name: String,
        val label: String,
    ) : AfternoteEditorIntent

    data class ReplaceReceiversIfEmpty(
        val receivers: List<AfternoteEditorReceiver>,
    ) : AfternoteEditorIntent

    data class ApplyPrefill(
        val prefill: EditorFormPrefill,
    ) : AfternoteEditorIntent

    data class InitializeProcessingMethodDefaults(
        val type: AfternoteType,
        val methods: List<String>,
    ) : AfternoteEditorIntent

    data class AddProcessingMethod(
        val text: String,
    ) : AfternoteEditorIntent

    data class DeleteProcessingMethod(
        val localId: Int,
    ) : AfternoteEditorIntent

    data class EditProcessingMethod(
        val localId: Int,
        val newText: String,
    ) : AfternoteEditorIntent

    data object RefreshAuthorReceivers : AfternoteEditorIntent

    data class UploadMemorialThumbnail(
        val jpegBytes: ByteArray?,
    ) : AfternoteEditorIntent

    data class MemorialThumbnailExtractionFailed(
        val throwable: Throwable,
    ) : AfternoteEditorIntent

    data object RetryMemorialThumbnail : AfternoteEditorIntent

    data class MemorialCaptureLaunchFailed(
        val throwable: Throwable,
    ) : AfternoteEditorIntent

    data class Save(
        val payload: RegisterAfternotePayload,
        val selectedReceiverIds: List<Long>,
        val memorialMedia: SaveAfternoteMemorialMedia,
        val asDraft: Boolean = false,
    ) : AfternoteEditorIntent

    data object RetryPrefill : AfternoteEditorIntent

    data object ConsumePrefill : AfternoteEditorIntent

    data object ConsumeSaveSuccess : AfternoteEditorIntent

    data object ConsumeThumbnailUploaded : AfternoteEditorIntent

    data class ConsumeError(
        val consumed: AfternoteEditorErrorEvent,
    ) : AfternoteEditorIntent

    data class ReceiversSelected(
        val receiverIds: List<Long>,
    ) : AfternoteEditorIntent

    data object ApplyPendingReceiverSelection : AfternoteEditorIntent
}

/** ViewModel의 작업 결과와 순수 폼 전이. */
internal sealed interface AfternoteEditorReducerEvent : ReducerEvent {
    data class TypeChanged(
        val type: AfternoteType,
    ) : AfternoteEditorReducerEvent

    data class ServiceChanged(
        val service: String,
    ) : AfternoteEditorReducerEvent

    data class MemorialPhotoChanged(
        val uri: String,
    ) : AfternoteEditorReducerEvent

    data object MemorialPhotoRemoved : AfternoteEditorReducerEvent

    data class MemorialVideoChanged(
        val url: String,
    ) : AfternoteEditorReducerEvent

    data object MemorialVideoRemoved : AfternoteEditorReducerEvent

    data class MemorialThumbnailChanged(
        val dataUrl: String,
    ) : AfternoteEditorReducerEvent

    data class PlaylistSongsAdded(
        val songs: List<Song>,
    ) : AfternoteEditorReducerEvent

    data class PlaylistSongsRemoved(
        val selectionKeys: Set<String>,
    ) : AfternoteEditorReducerEvent

    data object PlaylistSongsCleared : AfternoteEditorReducerEvent

    data class ReceiverDeleted(
        val receiverId: Long,
    ) : AfternoteEditorReducerEvent

    data class ReceiverAdded(
        val receiverId: Long,
        val name: String,
        val label: String,
    ) : AfternoteEditorReducerEvent

    data class EmptyReceiversReplaced(
        val receivers: List<AfternoteEditorReceiver>,
    ) : AfternoteEditorReducerEvent

    data class PrefillApplied(
        val prefill: EditorFormPrefill,
    ) : AfternoteEditorReducerEvent

    data class ProcessingMethodsInitialized(
        val type: AfternoteType,
        val methods: List<String>,
    ) : AfternoteEditorReducerEvent

    data class ProcessingMethodAdded(
        val text: String,
    ) : AfternoteEditorReducerEvent

    data class ProcessingMethodDeleted(
        val localId: Int,
    ) : AfternoteEditorReducerEvent

    data class ProcessingMethodEdited(
        val localId: Int,
        val newText: String,
    ) : AfternoteEditorReducerEvent

    data class Initialized(
        val form: EditorFormState,
        val originalType: AfternoteType?,
        val isPrefillLoading: Boolean,
    ) : AfternoteEditorReducerEvent

    data class AuthorReceiversLoaded(
        val receivers: List<AfternoteEditorReceiver>,
    ) : AfternoteEditorReducerEvent

    data class ThumbnailUploaded(
        val url: String,
    ) : AfternoteEditorReducerEvent

    data object ThumbnailExtractionRetried : AfternoteEditorReducerEvent

    data class ErrorRaised(
        val error: AfternoteEditorError,
    ) : AfternoteEditorReducerEvent

    data object SaveStarted : AfternoteEditorReducerEvent

    data class SaveSucceeded(
        val id: Long,
    ) : AfternoteEditorReducerEvent

    data class SaveFailed(
        val error: AfternoteEditorError,
    ) : AfternoteEditorReducerEvent

    data object PrefillStarted : AfternoteEditorReducerEvent

    data class PrefillLoaded(
        val prefill: EditorFormPrefill,
        val baseline: AfternoteEditorSnapshot,
        val preserveRestoredForm: Boolean,
    ) : AfternoteEditorReducerEvent

    data object PrefillFailed : AfternoteEditorReducerEvent

    data class SelectedReceiversApplied(
        val receivers: List<AfternoteEditorReceiver>,
    ) : AfternoteEditorReducerEvent

    data object PrefillConsumed : AfternoteEditorReducerEvent

    data object SaveSuccessConsumed : AfternoteEditorReducerEvent

    data object ThumbnailUploadConsumed : AfternoteEditorReducerEvent

    data class ErrorConsumed(
        val consumed: AfternoteEditorErrorEvent,
    ) : AfternoteEditorReducerEvent
}
