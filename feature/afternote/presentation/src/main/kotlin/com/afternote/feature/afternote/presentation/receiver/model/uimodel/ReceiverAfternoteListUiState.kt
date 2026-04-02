package com.afternote.feature.afternote.presentation.receiver.model.uimodel
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

/**
 * Receiver list screen UI state. Same shape as writer main (items, selectedTab, selectedNavItem).
 */
data class ReceiverAfternoteListUiState(
    val selectedTab: AfternoteCategory = AfternoteCategory.ALL,
    val selectedBottomNavItem: BottomNavTab = BottomNavTab.NOTE,
    val items: List<ListItemUiModel> = emptyList(),
)
