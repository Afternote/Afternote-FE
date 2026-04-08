package com.afternote.feature.afternote.presentation.shared.body.infinite

import androidx.compose.runtime.Stable
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

@Stable
data class AfternoteBodyUiState(
    val visibleItems: List<ListItemUiModel>,
    val selectedCategory: AfternoteCategory = AfternoteCategory.ALL,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
    val paginationError: String? = null,
)
