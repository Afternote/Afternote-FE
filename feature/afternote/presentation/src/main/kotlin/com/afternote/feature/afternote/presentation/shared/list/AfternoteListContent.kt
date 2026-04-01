package com.afternote.feature.afternote.presentation.shared.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.list.AfternoteCategoryRow
import com.afternote.feature.afternote.presentation.shared.list.content.EmptyAfternoteContent

private const val LOAD_MORE_THRESHOLD = 3

@Stable
data class AfternoteListContentUiState(
    val items: List<AfternoteItemUiModel>,
    val selectedTab: AfternoteCategory = AfternoteCategory.ALL,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)

@Composable
fun AfternoteListContent(
    uiState: AfternoteListContentUiState,
    onTabSelected: (AfternoteCategory) -> Unit,
    onItemClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        AfternoteCategoryRow(
            onCategorySelected = onTabSelected,
            selectedCategory = uiState.selectedTab,
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (uiState.items.isEmpty() && uiState.selectedTab == AfternoteCategory.ALL) {
            EmptyAfternoteContent(
                modifier =
                    Modifier
                        .fillMaxWidth(),
            )
        } else {
            AfternoteInfiniteList(
                uiState = uiState,
                onItemClick = onItemClick,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun AfternoteInfiniteList(
    uiState: AfternoteListContentUiState,
    onItemClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = uiState.items, key = { it.id }) { item ->
                AfternoteListItem(
                    item = item,
                    onClick = { onItemClick(item.id) },
                )
            }
            if (uiState.hasNext && uiState.isLoadingMore) {
                item(key = "loading_indicator") {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(32.dp))
                    }
                }
            }
        }
    }
    if (uiState.hasNext && !uiState.isLoadingMore && uiState.items.isNotEmpty()) {
        LaunchedEffect(listState, uiState.items.size) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .collect { visible ->
                    val lastIndex = visible.lastOrNull()?.index ?: return@collect
                    if (lastIndex >= uiState.items.size - LOAD_MORE_THRESHOLD) {
                        onLoadMore()
                    }
                }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListContentEmptyPreview() {
    AfternoteTheme {
        AfternoteListContent(
            uiState =
                AfternoteListContentUiState(
                    items = emptyList(),
                    selectedTab = AfternoteCategory.ALL,
                ),
            onTabSelected = {},
            onItemClick = {},
            onLoadMore = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListContentWithItemsPreview() {
    AfternoteTheme {
        AfternoteListContent(
            uiState =
                AfternoteListContentUiState(
                    items =
                        listOf(
                            AfternoteItemUiModel(
                                id = "1",
                                serviceName = "추모 가이드라인",
                                date = "2025.12.01",
                                iconResId = R.drawable.img_logo,
                            ),
                            AfternoteItemUiModel(
                                id = "2",
                                serviceName = "인스타그램",
                                date = "2025.11.26",
                                iconResId = R.drawable.img_logo,
                            ),
                        ),
                    selectedTab = AfternoteCategory.ALL,
                ),
            onTabSelected = {},
            onItemClick = {},
            onLoadMore = {},
        )
    }
}
