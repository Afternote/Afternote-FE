package com.afternote.feature.timeletter.presentation.viewmodel

import android.net.Uri
import androidx.compose.ui.text.style.TextAlign

data class TimeLetterWriteUiState(
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
)

sealed class EditorBlock {
    abstract val id: Long

    data class Text(
        override val id: Long,
    ) : EditorBlock()

    data class Image(
        override val id: Long,
        val uri: Uri,
        val name: String,
    ) : EditorBlock()

    data class Audio(
        override val id: Long,
        val uri: Uri,
        val name: String,
    ) : EditorBlock()

    data class File(
        override val id: Long,
        val uri: Uri,
        val name: String,
    ) : EditorBlock()

    data class Link(
        override val id: Long,
        val url: String,
    ) : EditorBlock()
}
