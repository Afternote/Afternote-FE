package com.afternote.feature.afternote.presentation.shared.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.home.HomeBodySectionSpacing
import com.afternote.feature.afternote.presentation.home.HomeBodyTopSpacing
import com.afternote.feature.afternote.presentation.home.HomeHeaderSection
import com.afternote.feature.afternote.presentation.home.NextStep
import com.afternote.feature.afternote.presentation.shared.component.AfternoteListContent

/**
 * @param filterRowScrollState 카테고리 필터 행의 가로 스크롤 위치. 화면이 `when` 위에서 만들어 여기로
 *   꿰는 값이다 — 근거는 [com.afternote.feature.afternote.presentation.author.home.AfternoteTypeFilterRow]
 *   KDoc (#1635). 기본값을 두지 않는 이유는 [AfternoteListContent] 의 같은 파라미터와 같다.
 */
@Composable
fun InfiniteListBody(
    items: LazyPagingItems<ListItemUiModel>,
    selectedType: AfternoteType?,
    onTypeSelected: (AfternoteType?) -> Unit,
    onListItemClick: (id: Long, type: AfternoteType) -> Unit,
    headerDescription: String,
    nextStep: NextStep?,
    filterRowScrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HomeBodySectionSpacing),
    ) {
        Spacer(Modifier.height(HomeBodyTopSpacing))
        HomeHeaderSection(
            description = headerDescription,
            nextStep = nextStep,
        )
        AfternoteListContent(
            items = items,
            selectedType = selectedType,
            onTypeSelected = onTypeSelected,
            onListItemClick = onListItemClick,
            filterRowScrollState = filterRowScrollState,
        )
    }
}
