package com.afternote.feature.afternote.presentation.shared.list

import androidx.compose.runtime.Stable

@Stable
data class AfternoteBodyUiState(
    val items: List<AfternoteItemUiModel>,
    val selectedTab: AfternoteCategory = AfternoteCategory.ALL,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)
