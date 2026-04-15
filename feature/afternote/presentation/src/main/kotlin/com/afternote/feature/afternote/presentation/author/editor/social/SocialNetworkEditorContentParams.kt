package com.afternote.feature.afternote.presentation.author.editor.social

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.author.editor.account.AccountSection
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection

/**
 * SocialNetworkEditorContent 함수의 매개변수를 묶는 data class
 */
@Immutable
data class SocialNetworkEditorContentParams(
    val editorMessages: List<EditorMessage>,
    val onMessageRegisterClick: (EditorMessage) -> Unit = {},
    val onMessageDeleteClick: (EditorMessage) -> Unit = {},
    val onMessageAddClick: () -> Unit = {},
    val accountSection: AccountSection,
    val recipientSection: AfternoteEditorReceiverSection? = null,
    val processingMethodSection: ProcessingMethodSection = ProcessingMethodSection(),
)
