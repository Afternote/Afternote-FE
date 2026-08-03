package com.afternote.feature.afternote.presentation.shared.body.infinite.content.list

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.AfternoteListItem
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf

/** append 단계 로딩만 푸터 스피너로 표시한다. 초기 로딩·새로고침·에러는 호출자에서 처리. */
@Composable
fun AfternoteList(
    items: LazyPagingItems<ListItemUiModel>,
    onItemClick: (id: String, type: AfternoteType) -> Unit,
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

@Preview(showBackground = true)
@Composable
private fun AfternoteListPreview() {
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
                            type = AfternoteType.SOCIAL_NETWORK,
                        ),
                        ListItemUiModel(
                            id = "2",
                            serviceName = "페이스북",
                            date = "2023.11.25",
                            iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteType.SOCIAL_NETWORK,
                        ),
                        ListItemUiModel(
                            id = "3",
                            serviceName = "트위터",
                            date = "2023.11.26",
                            iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteType.SOCIAL_NETWORK,
                        ),
                    ),
                ),
            ).collectAsLazyPagingItems()
        AfternoteList(
            items = items,
            onItemClick = { _, _ -> },
        )
    }
}
