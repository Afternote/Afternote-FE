package com.afternote.feature.afternote.presentation.shared.body.infinite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.home.HomeHeaderSection
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

@Composable
fun InfiniteListBody(
    uiState: AfternoteBodyUiState,
    onCategorySelected: (AfternoteCategory) -> Unit,
    onListItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    nextStepText: String = "",
    onLoadMore: () -> Unit = {},
    onNextStepClick: () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        HomeHeaderSection(
            nextStepText = nextStepText,
            onNextStepClick = onNextStepClick,
        )
        AfternoteListContent(
            uiState = uiState,
            onCategorySelected = onCategorySelected,
            onListItemClick = onListItemClick,
            onLoadMore = onLoadMore,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfiniteListBodyPreview() {
    AfternoteTheme {
        InfiniteListBody(
            nextStepText =
                "가족들의 '주거래 은행' 정보를\n" +
                    "입력하신 건 확인하셨나요?",
            uiState =
                AfternoteBodyUiState(
                    visibleItems =
                        listOf(
                            ListItemUiModel(
                                id = "1",
                                serviceName = "인스타그램",
                                date = "2023.11.24",
                                iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            ),
                            ListItemUiModel(
                                id = "2",
                                serviceName = "페이스북",
                                date = "2023.11.25",
                                iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            ),
                            ListItemUiModel(
                                id = "3",
                                serviceName = "갤러리",
                                date = "2023.11.26",
                                iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            ),
                        ),
                    selectedCategory = AfternoteCategory.ALL,
                    hasNext = true,
                    isLoadingMore = false,
                ),
            onCategorySelected = {},
            onListItemClick = {},
        )
    }
}
