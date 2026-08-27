package com.afternote.feature.afternote.presentation.shared.body.infinite.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.home.AfternoteTypeFilterRow
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.AfternoteList
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf
import com.afternote.core.ui.R as CoreUiR

@Composable
fun AfternoteListContent(
    items: LazyPagingItems<ListItemUiModel>,
    selectedType: AfternoteType?,
    onTypeSelected: (AfternoteType?) -> Unit,
    onListItemClick: (id: Long, type: AfternoteType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        AfternoteTypeFilterRow(
            onTabSelected = onTypeSelected,
            selectedTab = selectedType,
        )
        if (items.itemCount == 0) {
            // 카테고리 필터 결과 0건 — 카테고리 행은 유지한 채 안내 문구만 표시한다.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.afternote_home_filtered_empty),
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        } else {
            AfternoteList(
                items = items,
                onItemClick = onListItemClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListContentPreview() {
    AfternoteTheme {
        val items =
            flowOf(
                PagingData.from(
                    listOf(
                        ListItemUiModel(
                            id = 1L,
                            serviceName = "추억 노트",
                            date = "2025.12.01",
                            iconResId = CoreUiR.drawable.core_ui_afternote_logo,
                            type = AfternoteType.MEMORIAL,
                        ),
                        ListItemUiModel(
                            id = 2L,
                            serviceName = "인스타그램",
                            date = "2025.11.26",
                            iconResId = CoreUiR.drawable.core_ui_afternote_logo,
                            type = AfternoteType.SOCIAL_NETWORK,
                        ),
                    ),
                ),
            ).collectAsLazyPagingItems()
        AfternoteListContent(
            items = items,
            selectedType = AfternoteType.SOCIAL_NETWORK,
            onTypeSelected = {},
            onListItemClick = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListContentFilteredEmptyPreview() {
    AfternoteTheme {
        val items =
            flowOf(PagingData.empty<ListItemUiModel>()).collectAsLazyPagingItems()
        AfternoteListContent(
            items = items,
            selectedType = AfternoteType.SOCIAL_NETWORK,
            onTypeSelected = {},
            onListItemClick = { _, _ -> },
        )
    }
}
