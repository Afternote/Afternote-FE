package com.afternote.feature.afternote.presentation.author.edit.ui.content
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiverSection
import com.afternote.feature.afternote.presentation.author.edit.model.ProcessingMethodSection

/**
 * GalleryAndFileEditContent 함수의 매개변수를 묶는 data class
 */
@Immutable
data class GalleryAndFileEditContentParams(
    val messageState: TextFieldState,
    val recipientSection: AfternoteEditReceiverSection,
    val processingMethodSection: ProcessingMethodSection = ProcessingMethodSection(),
)
