package com.afternote.feature.afternote.presentation.author.editor.social
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.author.editor.model.AccountSection
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection

/**
 * SocialNetworkEditorContent 함수의 매개변수를 묶는 data class
 */
@Immutable
data class SocialNetworkEditorContentParams(
    val messageTitleState: TextFieldState,
    val messageState: TextFieldState,
    val accountSection: AccountSection,
    val recipientSection: AfternoteEditorReceiverSection? = null,
    val processingMethodSection: ProcessingMethodSection = ProcessingMethodSection(),
)
