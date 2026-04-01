package com.afternote.feature.afternote.presentation.author.list.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteListContentListParams
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteListScreenShell
import com.afternote.feature.afternote.presentation.shared.component.list.AfternoteTab
import com.afternote.feature.afternote.presentation.shared.model.dummy.afternote.AfternoteListDummies
import com.afternote.feature.afternote.presentation.shared.model.uimodel.AfternoteListDisplayItem
import com.afternote.feature.afternote.presentation.shared.model.util.getIconResForServiceName

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
    onNavTabSelected: (BottomNavTab) -> Unit,
) {
    AfternoteListScreenShell(
        modifier = modifier,
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
        onNavTabSelected = onNavTabSelected,
    )
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListScreenPreview() {
    AfternoteTheme {
        val sampleItems =
            AfternoteListDummies.defaultAfternoteList().mapIndexed { index, (name, date) ->
                AfternoteListDisplayItem(
                    id = index.toString(),
                    serviceName = name,
                    date = date,
                    iconResId = getIconResForServiceName(name),
                )
            }
        AfternoteListScreen(
            shell =
                AfternoteListScreenShellParams(
                    onBottomBarItemSelected = {},
                ),
            list =
                AfternoteListScreenListParams(
                    items = sampleItems,
                    onTabSelected = {},
                    onItemClick = {},
                ),
            onNavTabSelected = {},
        )
    }
}
