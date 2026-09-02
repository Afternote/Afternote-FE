package com.afternote.feature.afternote.presentation.editor.processing

import androidx.compose.runtime.Immutable

/**
 * 처리 방법 리스트 섹션 (공통)
 */
@Immutable
data class ProcessingMethodSection(
    val items: List<ProcessingMethodItem>,
    val onItemDeleteClick: (localId: Int) -> Unit,
    val onItemAdded: (text: String) -> Unit,
    val onItemEdited: (localId: Int, newText: String) -> Unit,
)
