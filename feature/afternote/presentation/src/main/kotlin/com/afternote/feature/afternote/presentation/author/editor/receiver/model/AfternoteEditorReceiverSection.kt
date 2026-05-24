package com.afternote.feature.afternote.presentation.author.editor.receiver.model
import androidx.compose.runtime.Immutable

/**
 * 수신자 섹션
 */
@Immutable
data class AfternoteEditorReceiverSection(
    val afternoteEditReceivers: List<AfternoteEditorReceiver> = emptyList(),
    val callbacks: AfternoteEditorReceiverCallbacks = AfternoteEditorReceiverCallbacks(),
)
