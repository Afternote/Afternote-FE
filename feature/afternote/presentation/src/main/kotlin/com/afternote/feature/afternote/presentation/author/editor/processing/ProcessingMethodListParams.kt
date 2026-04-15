package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem

/**
 * ProcessingMethodList 함수의 매개변수를 묶는 data class
 */
@Immutable
data class ProcessingMethodListParams(
    val items: List<ProcessingMethodItem>,
    val onItemDeleteClick: (String) -> Unit = {},
    val onItemAdded: (String) -> Unit = {},
    val onTextFieldVisibilityChanged: (Boolean) -> Unit = {},
    val onItemEdited: (String, String) -> Unit = { _, _ -> },
    val initialShowTextField: Boolean = false,
    val initialExpandedItemId: String? = null,
)
