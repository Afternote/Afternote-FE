package com.afternote.feature.afternote.presentation.receiver.ui.navgraph
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.feature.afternote.presentation.author.list.screen.AfternoteListScreen
import com.afternote.feature.afternote.presentation.author.list.screen.AfternoteListScreenListState
import com.afternote.feature.afternote.presentation.author.list.screen.AfternoteListScreenShellState
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
        listState = AfternoteListScreenListState(
            items = uiState.items,
            selectedTab = uiState.selectedTab,
        ),
        shellState =
            AfternoteListScreenShellState(
                bottomBarSelectedItem = uiState.selectedBottomNavItem,
            ),
        modifier = modifier,
    )
}
