package com.afternote.feature.afternote.presentation.receiver.model.uimodel
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteTab
import com.afternote.feature.afternote.presentation.shared.model.uimodel.AfternoteListDisplayItem

/**
 * Receiver list screen UI state. Same shape as writer main (items, selectedTab, selectedNavItem).
 */
data class ReceiverAfternoteListUiState(
    val selectedTab: AfternoteTab = AfternoteTab.ALL,
    val selectedBottomNavItem: BottomNavTab = BottomNavTab.NOTE,
    val items: List<AfternoteListDisplayItem> = emptyList(),
)
