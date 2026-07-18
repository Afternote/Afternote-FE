package com.afternote.feature.timeletter.presentation.viewmodel

import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import com.afternote.feature.timeletter.domain.model.RecordedAudio

data class TimeLetterWriteUiState(
    val editingTimeLetterId: Long? = null,
    val isLoadingEditingLetter: Boolean = false,
    val initialTitle: String? = null,
    val initialTextContents: Map<Long, String> = emptyMap(),
    val recipientIds: List<Long> = emptyList(),
    val recipientNames: List<String> = emptyList(),
    val sendAt: String? = null,
    val sendTime: String? = null,
    val sendHour: Int = 0,
    val sendMinute: Int = 0,
    val draftCount: Int = 0,
    val isSaving: Boolean = false,
    val textAlign: TextAlign = TextAlign.Start,
    val errorMessage: String? = null,
    val editorBlocks: List<EditorBlock> = listOf(EditorBlock.Text(id = 0L)),
    val focusedBlockId: Long? = 0L,
    val nextBlockId: Long = 1L,
    val savedAsDraft: Boolean = false,
    val registered: Boolean = false,
    val showVoiceRecorder: Boolean = false,
    val voiceRecordingState: VoiceRecordingState = VoiceRecordingState.Idle,
    val showFreePlanLimitPopup: Boolean = false,
)

sealed interface VoiceRecordingState {
    data object Idle : VoiceRecordingState

    data class Recording(
        val elapsedMillis: Long,
    ) : VoiceRecordingState

    data class Recorded(
        val audio: RecordedAudio,
    ) : VoiceRecordingState
}

sealed class EditorBlock {
    abstract val id: Long

    data class Text(
        override val id: Long,
    ) : EditorBlock()

    data class Image(
        override val id: Long,
        val uri: Uri,
        val name: String,
        val mimeType: String? = null,
    ) : EditorBlock()

    data class Audio(
        override val id: Long,
        val uri: Uri,
        val name: String,
        val mimeType: String? = null,
    ) : EditorBlock()

    data class File(
        override val id: Long,
        val uri: Uri,
        val name: String,
        val mimeType: String? = null,
    ) : EditorBlock()

    data class Link(
        override val id: Long,
        val url: String,
    ) : EditorBlock()
}
