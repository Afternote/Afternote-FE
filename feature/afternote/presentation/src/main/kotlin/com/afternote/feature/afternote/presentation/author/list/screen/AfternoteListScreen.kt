package com.afternote.feature.afternote.presentation.author.list.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteListContentListParams
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteListScreenShell
import com.afternote.feature.afternote.presentation.shared.model.uimodel.AfternoteListDisplayItem

/** Shell params for AfternoteListScreen (title, bottom bar, FAB). */
data class AfternoteListScreenShellState(
    val title: String = "애프터노트",
    val bottomBarSelectedItem: BottomNavTab = BottomNavTab.NOTE,
    val showFab: Boolean = false,
)

/** List params for AfternoteListScreen (items, tab, callbacks, pagination). */
data class AfternoteListScreenListState(
    val items: List<AfternoteListDisplayItem>,
    val selectedTab: AfternoteCategory = AfternoteCategory.ALL,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)

/**
 * Single shared screen for 애프터노트 list (writer main and receiver list).
 * Writer and receiver both call this with showFab true/false and their own listState/callbacks.
 */

@Suppress("LongParameterList")
@Composable
fun AfternoteListScreen(
    listState: AfternoteListScreenListState,
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
                list =
                    AfternoteListContentListParams(
                        items = listState.items,
                        selectedTab = listState.selectedTab,
                        onTabSelected = onTabSelected,
                        onItemClick = onItemClick,
                        hasNext = listState.hasNext,
                        isLoadingMore = listState.isLoadingMore,
                        onLoadMore = onLoadMore,
                    ),
            )
        },
        onNavTabSelected = onNavTabSelected,
    )
}
