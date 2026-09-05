package com.afternote.feature.afternote.presentation.shared.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.component.AfternoteListItem

/** append 단계 로딩만 푸터 스피너로 표시한다. 초기 로딩·새로고침·에러는 호출자에서 처리. */
@Composable
fun AfternoteList(
    items: LazyPagingItems<ListItemUiModel>,
    onItemClick: (id: Long, type: AfternoteType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            count = items.itemCount,
            key = items.itemKey { it.id },
            contentType = items.itemContentType { CONTENT_TYPE_LIST_ITEM },
        ) { index ->
            val item = items[index] ?: return@items
            AfternoteListItem(uiModel = item) { onItemClick(item.id, item.type) }
        }
        if (items.loadState.append is LoadState.Loading) {
            item(key = "append_loading") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                }
            }
        }
    }
}

private const val CONTENT_TYPE_LIST_ITEM = "afternote_list_item"
