package com.afternote.feature.afternote.presentation.shared.body.infinite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.home.HomeHeaderSection
import com.afternote.feature.afternote.presentation.author.home.NextStep
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.AfternoteListContent
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf

@Composable
fun InfiniteListBody(
    items: LazyPagingItems<ListItemUiModel>,
    selectedCategory: AfternoteType?,
    onCategorySelected: (AfternoteType?) -> Unit,
    onListItemClick: (id: Long, type: AfternoteType) -> Unit,
    headerDescription: String,
    nextStep: NextStep?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        HomeHeaderSection(
            description = headerDescription,
            nextStep = nextStep,
        )
        AfternoteListContent(
            items = items,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            onListItemClick = onListItemClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InfiniteListBodyPreview() {
    AfternoteTheme {
        val items =
            flowOf(
                PagingData.from(
                    listOf(
                        ListItemUiModel(
                            id = 1L,
                            serviceName = "인스타그램",
                            date = "2023.11.24",
                            iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteType.SOCIAL_NETWORK,
                        ),
                        ListItemUiModel(
                            id = 2L,
                            serviceName = "페이스북",
                            date = "2023.11.25",
                            iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteType.SOCIAL_NETWORK,
                        ),
                        ListItemUiModel(
                            id = 3L,
                            serviceName = "갤러리",
                            date = "2023.11.26",
                            iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteType.GALLERY_AND_FILES,
                        ),
                    ),
                ),
            ).collectAsLazyPagingItems()
        InfiniteListBody(
            headerDescription = stringResource(R.string.afternote_home_header_description),
            nextStep =
                NextStep(
                    text =
                        "가족들의 '주거래 은행' 정보를\n" +
                            "입력하신 건 확인하셨나요?",
                    onClick = {},
                ),
            items = items,
            selectedCategory = null,
            onCategorySelected = {},
            onListItemClick = { _, _ -> },
        )
    }
}
