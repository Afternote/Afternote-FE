package com.afternote.feature.afternote.presentation.author.editor.model
import androidx.compose.runtime.Immutable

/**
 * 수신자 관련 콜백을 묶는 data class
 */
@Immutable
data class AfternoteEditorReceiverCallbacks(
    val onAddClick: () -> Unit = {},
    val onItemDeleteClick: (String) -> Unit = {},
    val onItemAdded: (String) -> Unit = {},
    val onTextFieldVisibilityChanged: (Boolean) -> Unit = {},
)
