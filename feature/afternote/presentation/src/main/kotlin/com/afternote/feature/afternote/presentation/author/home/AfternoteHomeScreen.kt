package com.afternote.feature.afternote.presentation.author.home

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
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

@Suppress("LongParameterList", "kotlin:S107")
@Composable
fun AfternoteHomeScreen(
    listState: AfternoteBodyUiState,
    onNavTabSelected: (BottomNavTab) -> Unit,
    onCategorySelected: (AfternoteCategory) -> Unit,
    onListItemClick: (String) -> Unit,
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
        if (listState.visibleItems.isNotEmpty()) {
            InfiniteListBody(
                modifier = Modifier.padding(paddingValues),
                uiState = listState,
                onCategorySelected = onCategorySelected,
                onListItemClick = onListItemClick,
                onLoadMore = onLoadMore,
            )
        } else {
            EmptyListBody(modifier = Modifier.padding(paddingValues))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteHomeScreenPreview() {
    AfternoteTheme {
        AfternoteHomeScreen(
            listState =
                AfternoteBodyUiState(
                    visibleItems =
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
                    selectedCategory = AfternoteCategory.ALL,
                ),
            onNavTabSelected = {},
            onCategorySelected = {},
            onListItemClick = {},
            selectedNavTab = BottomNavTab.NOTE,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteHomeScreenEmptyPreview() {
    AfternoteTheme {
        AfternoteHomeScreen(
            listState =
                AfternoteBodyUiState(
                    visibleItems = emptyList(),
                    selectedCategory = AfternoteCategory.ALL,
                ),
            onNavTabSelected = {},
            onCategorySelected = {},
            onListItemClick = {},
            selectedNavTab = BottomNavTab.NOTE,
        )
    }
}
