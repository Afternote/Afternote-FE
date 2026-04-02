package com.afternote.feature.afternote.presentation.shared.body

import androidx.compose.runtime.Stable
import com.afternote.feature.afternote.presentation.shared.body.list.item.ListItemUiModel

@Stable
data class AfternoteBodyUiState(
    val items: List<ListItemUiModel>,
    val selectedTab: AfternoteCategory = AfternoteCategory.ALL,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)
