package com.afternote.feature.afternote.presentation.receiver.ui.navgraph
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.feature.afternote.presentation.author.list.screen.AfternoteListScreen
import com.afternote.feature.afternote.presentation.author.list.screen.AfternoteListScreenListParams
import com.afternote.feature.afternote.presentation.author.list.screen.AfternoteListScreenShellParams
import com.afternote.feature.afternote.presentation.receiver.model.ReceiverAfternoteListEvent
import com.afternote.feature.afternote.presentation.receiver.model.uimodel.ReceiverAfternoteListUiState

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
        modifier = modifier,
        shell =
            AfternoteListScreenShellParams(
                title = "애프터노트",
                bottomBarSelectedItem = uiState.selectedBottomNavItem,
                onBottomBarItemSelected = { onEvent(ReceiverAfternoteListEvent.SelectBottomNav(it)) },
                showFab = false,
                onFabClick = {},
            ),
        list =
            AfternoteListScreenListParams(
                items = uiState.items,
                selectedTab = uiState.selectedTab,
                onTabSelected = { onEvent(ReceiverAfternoteListEvent.SelectTab(it)) },
                onItemClick = { onEvent(ReceiverAfternoteListEvent.ClickItem(it)) },
            ),
    )
}
