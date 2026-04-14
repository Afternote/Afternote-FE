package com.afternote.feature.afternote.presentation.receiver.home
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

/**
 * Receiver list screen UI state. Same shape as writer main (visibleItems, selectedCategory, selectedNavItem).
 */
data class ReceiverAfternoteHomeUiState(
    val selectedTab: AfternoteCategory = AfternoteCategory.ALL,
    val selectedBottomNavItem: BottomNavTab = BottomNavTab.NOTE,
    val visibleItems: List<ListItemUiModel> = emptyList(),
)
