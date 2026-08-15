package com.afternote.feature.timeletter.presentation.viewmodel

import android.net.Uri
import androidx.compose.ui.text.style.TextAlign

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
    val error: TimeLetterWriteError? = null,
    val editorBlocks: List<EditorBlock> = listOf(EditorBlock.Text(id = 0L)),
    val focusedBlockId: Long? = 0L,
    val nextBlockId: Long = 1L,
    val savedAsDraft: Boolean = false,
    val registered: Boolean = false,
    val showFreePlanLimitPopup: Boolean = false,
)

enum class TimeLetterWriteError {
    SEND_DATE_REQUIRED,
    LOAD_FAILED,
    RECIPIENT_REQUIRED,
    SAVE_FAILED,
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
