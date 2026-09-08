package com.afternote.feature.afternote.presentation.draft

import androidx.paging.PagingData
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel

internal data class AfternoteDraftListUiState(
    val items: PagingData<ListItemUiModel> = PagingData.empty(),
) : UiState

internal sealed interface AfternoteDraftListReducerEvent : ReducerEvent {
    data class PageGenerationChanged(
        val items: PagingData<ListItemUiModel>,
    ) : AfternoteDraftListReducerEvent
}
