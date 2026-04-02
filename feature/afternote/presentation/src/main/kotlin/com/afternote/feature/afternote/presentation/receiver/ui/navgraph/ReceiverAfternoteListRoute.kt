package com.afternote.feature.afternote.presentation.receiver.ui.navgraph
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.feature.afternote.presentation.author.list.screen.AfternoteListScreen
import com.afternote.feature.afternote.presentation.receiver.model.ReceiverAfternoteListEvent
import com.afternote.feature.afternote.presentation.receiver.model.uimodel.ReceiverAfternoteListUiState
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState

/**
 * Receiver list Route. Calls shared AfternoteListScreen with showFab = false.
 */
@Composable
fun ReceiverAfternoteListRoute(
    uiState: ReceiverAfternoteListUiState,
    onEvent: (ReceiverAfternoteListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AfternoteListScreen(
        listState =
            AfternoteBodyUiState(
                items = uiState.items,
                selectedTab = uiState.selectedTab,
            ),
        onNavTabSelected = { onEvent(ReceiverAfternoteListEvent.SelectBottomNav(it)) },
        onCategorySelected = { onEvent(ReceiverAfternoteListEvent.SelectTab(it)) },
        onListItemClick = { onEvent(ReceiverAfternoteListEvent.ClickItem(it)) },
        modifier = modifier,
        selectedNavTab = uiState.selectedBottomNavItem,
    )
}
