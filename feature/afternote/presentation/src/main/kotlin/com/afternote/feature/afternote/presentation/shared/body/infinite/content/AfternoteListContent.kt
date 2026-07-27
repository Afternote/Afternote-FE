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
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.home.AfternoteCategoryRow
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.AfternoteList
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf

@Composable
fun AfternoteListContent(
    items: LazyPagingItems<ListItemUiModel>,
    selectedCategory: AfternoteCategory,
    onCategorySelected: (AfternoteCategory) -> Unit,
    onListItemClick: (id: String, type: AfternoteServiceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        AfternoteCategoryRow(
            onTabSelected = onCategorySelected,
            selectedTab = selectedCategory,
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
                            id = "1",
                            serviceName = "추억 노트",
                            date = "2025.12.01",
                            iconResId = R.drawable.feature_afternote_img_logo,
                            type = AfternoteServiceType.MEMORIAL,
                        ),
                        ListItemUiModel(
                            id = "2",
                            serviceName = "인스타그램",
                            date = "2025.11.26",
                            iconResId = R.drawable.feature_afternote_img_logo,
                            type = AfternoteServiceType.SOCIAL_NETWORK,
                        ),
                    ),
                ),
            ).collectAsLazyPagingItems()
        AfternoteListContent(
            items = items,
            selectedCategory = AfternoteCategory.SOCIAL_NETWORK,
            onCategorySelected = {},
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
            selectedCategory = AfternoteCategory.SOCIAL_NETWORK,
            onCategorySelected = {},
            onListItemClick = { _, _ -> },
        )
    }
}
