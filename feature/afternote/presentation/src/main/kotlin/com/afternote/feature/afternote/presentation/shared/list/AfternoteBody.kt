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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.list.AfternoteCategoryRow
import com.afternote.feature.afternote.presentation.shared.list.content.AfternoteEmptyList

private const val LOAD_MORE_THRESHOLD = 3

@Composable
fun AfternoteBody(
    uiState: AfternoteBodyUiState,
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
        if (uiState.items.isEmpty()) {
            AfternoteEmptyList(
                modifier =
                    Modifier
                        .fillMaxWidth(),
            )
        } else {
            AfternoteInfiniteList(
                bodyUiState = uiState,
                onItemClick = onItemClick,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun AfternoteInfiniteList(
    bodyUiState: AfternoteBodyUiState,
    onItemClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // layoutInfo, 스크롤 위치, 상호작용 상태를 담고 있음
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = bodyUiState.items, key = { it.id }) { item ->
            AfternoteListItem(
                item = item,
                onClick = { onItemClick(item.id) },
            )
        }
        if (bodyUiState.hasNext && bodyUiState.isLoadingMore) {
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
    if (bodyUiState.hasNext && !bodyUiState.isLoadingMore && bodyUiState.items.isNotEmpty()) {
        // LazyColumn의 상태와
        LaunchedEffect(listState, bodyUiState.items.size) {
            // layoutInfo는 리스트의 물리적인 배치 정보를 담고 있음
            // visibleItemsInfo는 현재 화면에 보이고 있는 아이템들의 리스트
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .collect { visible ->
                    val lastIndex = visible.lastOrNull()?.index ?: return@collect
                    if (lastIndex >= bodyUiState.items.size - LOAD_MORE_THRESHOLD) {
                        onLoadMore()
                    }
                }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteBodyEmptyPreview() {
    AfternoteTheme {
        AfternoteBody(
            uiState =
                AfternoteBodyUiState(
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
private fun AfternoteBodyWithItemsPreview() {
    AfternoteTheme {
        AfternoteBody(
            uiState =
                AfternoteBodyUiState(
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
