package com.afternote.feature.afternote.presentation.author.list.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteListContentListParams
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteListScreenShell
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteTab
import com.afternote.feature.afternote.presentation.shared.model.uimodel.AfternoteListDisplayItem

/** Shell params for AfternoteListScreen (title, bottom bar, FAB). */
data class AfternoteListScreenShellParams(
    val title: String = "애프터노트",
    val bottomBarSelectedItem: BottomNavTab = BottomNavTab.NOTE,
    val onBottomBarItemSelected: (BottomNavTab) -> Unit,
    val showFab: Boolean = false,
    val onFabClick: () -> Unit = {},
)

/** List params for AfternoteListScreen (items, tab, callbacks, pagination). */
data class AfternoteListScreenListParams(
    val items: List<AfternoteListDisplayItem>,
    val selectedTab: AfternoteTab = AfternoteTab.ALL,
    val onTabSelected: (AfternoteTab) -> Unit,
    val onItemClick: (String) -> Unit,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
    val onLoadMore: () -> Unit = {},
)

/**
 * Single shared screen for 애프터노트 list (writer main and receiver list).
 * Writer and receiver both call this with showFab true/false and their own state/callbacks.
 */
@Composable
fun AfternoteListScreen(
    modifier: Modifier = Modifier,
    shell: AfternoteListScreenShellParams,
    list: AfternoteListScreenListParams,
) {
    AfternoteListScreenShell(
        modifier = modifier,
        title = shell.title,
        onTabClick = shell.bottomBarSelectedItem,
//        on = shell.onBottomBarItemSelected,
        showFab = shell.showFab,
        onFabClick = shell.onFabClick,
        content = { contentModifier ->
            AfternoteListContent(
                modifier = contentModifier,
                list =
                    AfternoteListContentListParams(
                        items = list.items,
                        selectedTab = list.selectedTab,
                        onTabSelected = list.onTabSelected,
                        onItemClick = list.onItemClick,
                        hasNext = list.hasNext,
                        isLoadingMore = list.isLoadingMore,
                        onLoadMore = list.onLoadMore,
                    ),
            )
        },
    )
}
