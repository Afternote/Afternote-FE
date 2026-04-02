package com.afternote.feature.afternote.presentation.shared.body.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.shared.body.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.list.item.AfternoteListItem

private const val LOAD_MORE_THRESHOLD = 3

@Composable
fun AfternoteInfiniteList(
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
        items(items = bodyUiState.items, key = { it.id }) { itemUiModel ->
            AfternoteListItem(
                uiModel = itemUiModel,
            ) { onItemClick(itemUiModel.id) }
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
