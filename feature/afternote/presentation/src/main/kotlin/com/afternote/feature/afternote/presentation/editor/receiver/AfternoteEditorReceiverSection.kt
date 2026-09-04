package com.afternote.feature.afternote.presentation.editor.receiver
import androidx.compose.runtime.Immutable

/**
 * 수신자 섹션
 */
@Immutable
data class AfternoteEditorReceiverSection(
    val afternoteEditReceivers: List<AfternoteEditorReceiver> = emptyList(),
    val onAddClick: () -> Unit,
    val onItemDeleteClick: (Long) -> Unit,
)
