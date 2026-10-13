package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.annotation.StringRes
import com.afternote.core.ui.mvi.UiState

internal sealed interface ReceivedAfternoteDetailUiState : UiState {
    data object Loading : ReceivedAfternoteDetailUiState

    data class Success(
        val detailId: Long,
        val contentUiModel: ReceivedDetailContentUiModel,
    ) : ReceivedAfternoteDetailUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : ReceivedAfternoteDetailUiState
}
