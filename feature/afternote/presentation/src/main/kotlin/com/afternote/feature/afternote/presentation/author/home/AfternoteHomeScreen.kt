package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.button.FAB.PenFloatingActionButton
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.body.LoadingListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "kotlin:S107")
@Composable
fun AfternoteHomeScreen(
    listState: AfternoteBodyUiState,
    onCategorySelected: (AfternoteCategory) -> Unit,
    onListItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onFabClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (listState.visibleItems.isNotEmpty()) {
                HomeTopBar()
            } else {
                DetailTopBar(title = "애프터노트")
            }
        },
        floatingActionButton = { PenFloatingActionButton(onClick = onFabClick) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = listState.isRefreshing,
            onRefresh = onRefresh,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            val bodyModifier = Modifier.fillMaxSize()
            when {
                listState.isLoading && listState.visibleItems.isEmpty() -> {
                    LoadingListBody(modifier = bodyModifier)
                }
                listState.visibleItems.isNotEmpty() -> {
                    InfiniteListBody(
                        modifier = bodyModifier,
                        uiState = listState,
                        onCategorySelected = onCategorySelected,
                        onListItemClick = onListItemClick,
                        onLoadMore = onLoadMore,
                    )
                }
                else -> {
                    EmptyListBody(modifier = bodyModifier)
                }
            }
        }
    }
}

@Preview
@Composable
private fun AfternoteHomeScreenPreview() {
    AfternoteTheme {
        AfternoteHomeScreen(
            listState =
                AfternoteBodyUiState(
                    isLoading = false,
                    visibleItems =
                        listOf(
                            ListItemUiModel(
                                id = "1",
                                serviceName = "인스타그램",
                                date = "2023.11.24",
                                iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            ),
                            ListItemUiModel(
                                id = "2",
                                serviceName = "페이스북",
                                date = "2023.11.25",
                                iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            ),
                        ),
                    selectedCategory = AfternoteCategory.ALL,
                ),
            onCategorySelected = {},
            onListItemClick = {},
        )
    }
}

@Preview
@Composable
private fun AfternoteHomeScreenEmptyPreview() {
    AfternoteTheme {
        AfternoteHomeScreen(
            listState =
                AfternoteBodyUiState(
                    isLoading = false,
                    visibleItems = emptyList(),
                    selectedCategory = AfternoteCategory.ALL,
                ),
            onCategorySelected = {},
            onListItemClick = {},
        )
    }
}
