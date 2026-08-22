package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.compose.runtime.Immutable

@Immutable
data class ReceivedRecordsUiState(
    val senders: List<ReceivedRecordItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false,
)
