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
    val attachments: List<LetterAttachment> = emptyList(),
)

sealed class LetterAttachment {
    data class ImageAttachment(val uri: Uri, val name: String) : LetterAttachment()
    data class AudioAttachment(val uri: Uri, val name: String) : LetterAttachment()
    data class FileAttachment(val uri: Uri, val name: String) : LetterAttachment()
    data class LinkAttachment(val url: String) : LetterAttachment()
}

sealed interface TimeLetterWriteEvent {
    data object SavedAsDraft : TimeLetterWriteEvent

    data object Registered : TimeLetterWriteEvent

    data class Error(
        val message: String,
    ) : TimeLetterWriteEvent
}
