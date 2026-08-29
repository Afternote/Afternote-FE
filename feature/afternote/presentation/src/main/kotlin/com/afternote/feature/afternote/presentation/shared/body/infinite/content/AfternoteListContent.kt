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
import androidx.paging.compose.LazyPagingItems
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.home.AfternoteTypeFilterRow
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.AfternoteList
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

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
