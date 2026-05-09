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
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.button.FAB.PenFloatingActionButton
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.body.LoadingListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "kotlin:S107")
@Composable
fun AfternoteHomeScreen(
    items: LazyPagingItems<ListItemUiModel>,
    selectedCategory: AfternoteCategory,
    isInitialLoading: Boolean,
    isRefreshing: Boolean,
    onCategorySelected: (AfternoteCategory) -> Unit,
    onListItemClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onFabClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (items.itemCount > 0) {
                HomeTopBar()
            } else {
                DetailTopBar(title = "애프터노트")
            }
        },
        floatingActionButton = { PenFloatingActionButton(onClick = onFabClick) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            val bodyModifier = Modifier.fillMaxSize()
            when {
                isInitialLoading -> {
                    LoadingListBody(modifier = bodyModifier)
                }

                items.itemCount > 0 -> {
                    InfiniteListBody(
                        modifier = bodyModifier,
                        items = items,
                        selectedCategory = selectedCategory,
                        onCategorySelected = onCategorySelected,
                        onListItemClick = onListItemClick,
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
        val items =
            flowOf(
                PagingData.from(
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
                ),
            ).collectAsLazyPagingItems()
        AfternoteHomeScreen(
            items = items,
            selectedCategory = AfternoteCategory.ALL,
            isInitialLoading = false,
            isRefreshing = false,
            onCategorySelected = {},
            onListItemClick = {},
            onRefresh = {},
        )
    }
}

@Preview
@Composable
private fun AfternoteHomeScreenEmptyPreview() {
    AfternoteTheme {
        val items =
            flowOf(PagingData.empty<ListItemUiModel>()).collectAsLazyPagingItems()
        AfternoteHomeScreen(
            items = items,
            selectedCategory = AfternoteCategory.ALL,
            isInitialLoading = false,
            isRefreshing = false,
            onCategorySelected = {},
            onListItemClick = {},
            onRefresh = {},
        )
    }
}
