package com.afternote.feature.afternote.presentation.shared.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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

/**
 * List params for [AfternoteListContent] (S107: keep param count ≤7).
 *
 * @param items Items to show
 * @param selectedTab Current tab
 * @param onTabSelected Tab selection callback
 * @param onItemClick Item click callback
 * @param hasNext Whether more pages exist
 * @param isLoadingMore Whether next page is loading
 * @param onLoadMore Callback when user scrolls near end and more can load
 */

@Stable
data class AfternoteListContentParams(
    val items: List<AfternoteItemUiModel>,
    val selectedTab: AfternoteCategory = AfternoteCategory.ALL,
    val onTabSelected: (AfternoteCategory) -> Unit = {},
    val onItemClick: (String) -> Unit = {},
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
    val onLoadMore: () -> Unit = {},
)

/**
 * Shared list content for 애프터노트 list screens (writer main and receiver list).
 * Same look: tab row, then empty state or list of items. Only FAB differs at shell level.
 * When [list.hasNext] is true and user scrolls near the end, [list.onLoadMore] is called.
 */
@Composable
fun AfternoteListContent(
    list: AfternoteListContentParams,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        AfternoteCategoryRow(
            onCategorySelected = list.onTabSelected,
            selectedCategory = list.selectedTab,
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (list.items.isEmpty() && list.selectedTab == AfternoteCategory.ALL) {
            EmptyAfternoteContent(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
        } else {
            AfternotePagedList(list = list)
        }
    }
}

@Composable
private fun AfternotePagedList(
    list: AfternoteListContentParams,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = list.items, key = { it.id }) { item ->
                AfternoteListItem(
                    item = item,
                    onClick = { list.onItemClick(item.id) },
                )
            }
        }
        if (list.hasNext && list.isLoadingMore) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                contentAlignment = Alignment.Center,
            ) {}
        }
    }
    if (list.hasNext && !list.isLoadingMore && list.items.isNotEmpty()) {
        LaunchedEffect(listState, list.items.size) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .collect { visible ->
                    val lastIndex = visible.lastOrNull()?.index ?: return@collect
                    if (lastIndex >= list.items.size - LOAD_MORE_THRESHOLD) {
                        list.onLoadMore()
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
            list =
                AfternoteListContentParams(
                    items = emptyList(),
                    selectedTab = AfternoteCategory.ALL,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListContentWithItemsPreview() {
    AfternoteTheme {
        AfternoteListContent(
            list =
                AfternoteListContentParams(
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
        )
    }
}
