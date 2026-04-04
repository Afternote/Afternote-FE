package com.afternote.feature.afternote.presentation.receiver.navgraph
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.feature.afternote.presentation.author.home.screen.AfternoteHomeScreen
import com.afternote.feature.afternote.presentation.receiver.model.ReceiverAfternoteHomeEvent
import com.afternote.feature.afternote.presentation.receiver.model.uistate.ReceiverAfternoteHomeUiState
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState

/**
 * Receiver list Route. Calls shared AfternoteHomeScreen with showFab = false.
 */
@Composable
fun ReceiverAfternoteHomeRoute(
    uiState: ReceiverAfternoteHomeUiState,
    onEvent: (ReceiverAfternoteHomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AfternoteHomeScreen(
        listState =
            AfternoteBodyUiState(
                items = uiState.items,
                selectedTab = uiState.selectedTab,
            ),
        onNavTabSelected = { onEvent(ReceiverAfternoteHomeEvent.SelectBottomNav(it)) },
        onCategorySelected = { onEvent(ReceiverAfternoteHomeEvent.SelectTab(it)) },
        onListItemClick = { onEvent(ReceiverAfternoteHomeEvent.ClickItem(it)) },
        modifier = modifier,
        selectedNavTab = uiState.selectedBottomNavItem,
    )
}
