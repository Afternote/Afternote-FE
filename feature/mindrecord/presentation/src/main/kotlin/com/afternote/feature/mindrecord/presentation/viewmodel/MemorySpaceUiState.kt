package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem

sealed interface MemorySpaceUiState {
    data object Loading : MemorySpaceUiState

    /** [memories] 가 비어 있으면 아직 기록이 없는 사용자다 — 화면이 빈 상태 안내를 띄운다. */
    data class Success(
        val memories: List<MemoryItem>,
    ) : MemorySpaceUiState

    data class Error(
        val message: UiText,
    ) : MemorySpaceUiState
}
