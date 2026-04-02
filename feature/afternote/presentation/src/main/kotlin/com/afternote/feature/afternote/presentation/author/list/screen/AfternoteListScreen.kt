package com.afternote.feature.afternote.presentation.author.list.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.scaffold.bottombar.BottomBar
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.core.ui.scaffold.topbar.HomeTopBar
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.AfternoteAddFAB
import com.afternote.feature.afternote.presentation.shared.body.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.body.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.body.list.item.ListItemUiModel

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
            HomeTopBar()
        },
        bottomBar = {
            BottomBar(
                selectedNavTab = selectedNavTab,
                onTabClick = onNavTabSelected,
            )
        },
        floatingActionButton = { AfternoteAddFAB(onClick = onFabClick) },
    ) { paddingValues ->
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
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListScreenPreview() {
    AfternoteTheme {
        AfternoteListScreen(
            listState =
                AfternoteBodyUiState(
                    items =
                        listOf(
                            ListItemUiModel(
                                id = "1",
                                serviceName = "인스타그램",
                                date = "2023.11.24",
                                iconResId = R.drawable.img_insta_pattern,
                            ),
                            ListItemUiModel(
                                id = "2",
                                serviceName = "페이스북",
                                date = "2023.11.25",
                                iconResId = R.drawable.img_insta_pattern,
                            ),
                        ),
                    selectedTab = AfternoteCategory.ALL,
                ),
            onNavTabSelected = {},
            onTabSelected = {},
            onItemClick = {},
            selectedNavTab = BottomNavTab.NOTE,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListScreenEmptyPreview() {
    AfternoteTheme {
        AfternoteListScreen(
            listState =
                AfternoteBodyUiState(
                    items = emptyList(),
                    selectedTab = AfternoteCategory.ALL,
                ),
            onNavTabSelected = {},
            onTabSelected = {},
            onItemClick = {},
            selectedNavTab = BottomNavTab.NOTE,
        )
    }
}
