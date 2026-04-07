package com.afternote.feature.afternote.presentation.author.editor.gallery

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection

/**
 * GalleryAndFileEditorContent 함수의 매개변수를 묶는 data class
 */
@Immutable
data class GalleryAndFileEditorContentParams(
    val editorMessages: List<EditorMessage>,
    val onMessageRegisterClick: (EditorMessage) -> Unit = {},
    val onMessageDeleteClick: (EditorMessage) -> Unit = {},
    val onMessageAddClick: () -> Unit = {},
    val recipientSection: AfternoteEditorReceiverSection,
    val processingMethodSection: ProcessingMethodSection = ProcessingMethodSection(),
)
