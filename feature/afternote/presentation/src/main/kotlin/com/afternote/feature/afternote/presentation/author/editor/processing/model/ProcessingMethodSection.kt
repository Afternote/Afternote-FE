package com.afternote.feature.afternote.presentation.author.editor.processing.model
import androidx.compose.runtime.Immutable

/**
 * 처리 방법 리스트 섹션 (공통)
 */
@Immutable
data class ProcessingMethodSection(
    val items: List<ProcessingMethodItem> = emptyList(),
    val onItemDeleteClick: (String) -> Unit = {},
    val onItemAdded: (String) -> Unit = {},
    val onTextFieldVisibilityChanged: (Boolean) -> Unit = {},
    val onItemEdited: (String, String) -> Unit = { _, _ -> },
)
