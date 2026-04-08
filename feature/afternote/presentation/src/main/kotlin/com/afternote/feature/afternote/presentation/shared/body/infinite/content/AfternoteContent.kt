@file:JvmName("InfiniteListContentKt")

package com.afternote.feature.afternote.presentation.shared.body.infinite.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.home.AfternoteCategoryRow
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.AfternoteList
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

@Composable
fun AfternoteListContent(
    uiState: AfternoteBodyUiState,
    onCategorySelected: (AfternoteCategory) -> Unit,
    onListItemClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        AfternoteCategoryRow(
            onTabSelected = onCategorySelected,
            selectedTab = uiState.selectedCategory,
        )
        AfternoteList(
            bodyUiState = uiState,
            onItemClick = onListItemClick,
            onLoadMore = onLoadMore,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListContentPreview() {
    AfternoteTheme {
        AfternoteListContent(
            uiState =
                AfternoteBodyUiState(
                    visibleItems =
                        listOf(
                            ListItemUiModel(
                                id = "1",
                                serviceName = "추모 가이드라인",
                                date = "2025.12.01",
                                iconResId = R.drawable.feature_afternote_img_logo,
                            ),
                            ListItemUiModel(
                                id = "2",
                                serviceName = "인스타그램",
                                date = "2025.11.26",
                                iconResId = R.drawable.feature_afternote_img_logo,
                            ),
                        ),
                    selectedCategory = AfternoteCategory.SOCIAL_NETWORK,
                ),
            onCategorySelected = {},
            onListItemClick = {},
            onLoadMore = {},
        )
    }
}
