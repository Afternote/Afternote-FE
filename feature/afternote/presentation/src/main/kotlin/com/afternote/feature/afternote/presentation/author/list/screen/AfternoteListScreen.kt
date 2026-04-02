package com.afternote.feature.afternote.presentation.author.list.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.scaffold.TopBar
import com.afternote.core.ui.scaffold.bottombar.BottomBar
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.list.AfternoteBody
import com.afternote.feature.afternote.presentation.shared.list.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.list.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.scaffold.ScaffoldContentWithOptionalFab

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
    listState: AfternoteBodyUiState,
    shellState: AfternoteListScreenShellState,
    onNavTabSelected: (BottomNavTab) -> Unit,
    onTabSelected: (AfternoteCategory) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {},
    onFabClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar()
        },
        bottomBar = {
            BottomBar(
                selectedNavTab = shellState.bottomBarSelectedItem,
                onTabClick = onNavTabSelected,
            )
        },
    ) { paddingValues ->
        ScaffoldContentWithOptionalFab(
            paddingValues = paddingValues,
            showFab = shellState.showFab,
            onFabClick = onFabClick,
        ) { contentModifier ->
            AfternoteBody(
                modifier = contentModifier,
                uiState = listState,
                onTabSelected = onTabSelected,
                onItemClick = onItemClick,
                onLoadMore = onLoadMore,
            )
        }
    }
}
