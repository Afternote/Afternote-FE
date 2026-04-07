package com.afternote.feature.afternote.presentation.author.editor.gallery
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection

/**
 * GalleryAndFileEditorContent 함수의 매개변수를 묶는 data class
 */
@Immutable
data class GalleryAndFileEditorContentParams(
    val messageTitleState: TextFieldState,
    val messageState: TextFieldState,
    val recipientSection: AfternoteEditorReceiverSection,
    val processingMethodSection: ProcessingMethodSection = ProcessingMethodSection(),
)
