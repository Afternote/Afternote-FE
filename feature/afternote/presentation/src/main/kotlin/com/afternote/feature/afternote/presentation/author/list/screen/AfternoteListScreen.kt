package com.afternote.feature.afternote.presentation.author.list.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.list.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.list.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.list.AfternoteListContentUiState
import com.afternote.feature.afternote.presentation.shared.list.AfternoteListScreenShell

/** Shell params for AfternoteListScreen (title, bottom bar, FAB). */
data class AfternoteListScreenShellState(
    val title: String = "애프터노트",
    val bottomBarSelectedItem: BottomNavTab = BottomNavTab.NOTE,
    val showFab: Boolean = false,
)

/**
 * Single shared screen for 애프터노트 list (writer main and receiver list).
 * Writer and receiver both call this with showFab true/false and their own listState/callbacks.
 */
@Suppress("LongParameterList")
@Composable
fun AfternoteListScreen(
    listState: AfternoteListContentUiState,
    shellState: AfternoteListScreenShellState,
    onNavTabSelected: (BottomNavTab) -> Unit,
    onTabSelected: (AfternoteCategory) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {},
    onFabClick: () -> Unit = {},
) {
    AfternoteListScreenShell(
        modifier = modifier,
        showFab = shellState.showFab,
        onFabClick = onFabClick,
        content = { contentModifier ->
            AfternoteListContent(
                modifier = contentModifier,
                uiState = listState,
                onTabSelected = onTabSelected,
                onItemClick = onItemClick,
                onLoadMore = onLoadMore,
            )
        },
        onNavTabSelected = onNavTabSelected,
    )
}
