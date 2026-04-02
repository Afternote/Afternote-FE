package com.afternote.feature.afternote.presentation.shared.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.list.AfternoteCategoryRow
import com.afternote.feature.afternote.presentation.shared.body.list.AfternoteList
import com.afternote.feature.afternote.presentation.shared.body.list.item.ListItemUiModel

@Composable
fun InfiniteListBody(
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
        AfternoteList(
            bodyUiState = uiState,
            onItemClick = onItemClick,
            onLoadMore = onLoadMore,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfiniteListBodyPreview() {
    AfternoteTheme {
        InfiniteListBody(
            uiState =
                AfternoteBodyUiState(
                    items =
                        listOf(
                            ListItemUiModel(
                                id = "1",
                                serviceName = "추모 가이드라인",
                                date = "2025.12.01",
                                iconResId = R.drawable.img_logo,
                            ),
                            ListItemUiModel(
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
