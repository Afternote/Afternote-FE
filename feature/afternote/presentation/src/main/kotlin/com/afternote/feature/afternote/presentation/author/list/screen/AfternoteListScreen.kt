package com.afternote.feature.afternote.presentation.author.list.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.scaffold.TopBar
import com.afternote.core.ui.scaffold.bottombar.BottomBar
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.body.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.body.InfiniteListBody

@Suppress("LongParameterList")
@Composable
fun AfternoteListScreen(
    listState: AfternoteBodyUiState,
    onNavTabSelected: (BottomNavTab) -> Unit,
    onTabSelected: (AfternoteCategory) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedNavTab: BottomNavTab = BottomNavTab.NOTE,
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
                selectedNavTab = selectedNavTab,
                onTabClick = onNavTabSelected,
            )
        },
    ) { paddingValues ->
//        ScaffoldContentWithOptionalFab(
//            paddingValues = paddingValues,
//            showFab = showFab,
//            onFabClick = onFabClick,
//        ) { contentModifier ->
        if (listState.items.isNotEmpty()) {
            InfiniteListBody(
                modifier = Modifier.padding(paddingValues),
                uiState = listState,
                onTabSelected = onTabSelected,
                onItemClick = onItemClick,
                onLoadMore = onLoadMore,
            )
        } else {
            EmptyListBody()
        }
//        }
    }
}
