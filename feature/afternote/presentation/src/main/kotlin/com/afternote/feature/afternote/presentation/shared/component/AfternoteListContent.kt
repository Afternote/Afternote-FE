package com.afternote.feature.afternote.presentation.shared.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.paging.compose.LazyPagingItems
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.home.AfternoteTypeFilterRow
import com.afternote.feature.afternote.presentation.shared.component.AfternoteList

/**
 * @param filterRowScrollState 카테고리 필터 행의 가로 스크롤 위치. 호출부가 넘기는 이유는
 *   [AfternoteTypeFilterRow] KDoc 참조 — 이 본문은 화면의 여러 본문 중 하나라, 행이 스크롤 위치를
 *   자기 안에 들고 있으면 카테고리 전환마다 0 으로 돌아간다 (#1635). 기본값을 두지 않는다 — 빠뜨려도
 *   컴파일이 통과하면 행이 조용히 자기 위치를 다시 들고 그 회귀가 렌더에 드러나지 않는다.
 */
@Composable
fun AfternoteListContent(
    items: LazyPagingItems<ListItemUiModel>,
    selectedType: AfternoteType?,
    onTypeSelected: (AfternoteType?) -> Unit,
    onListItemClick: (id: Long, type: AfternoteType) -> Unit,
    filterRowScrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        AfternoteTypeFilterRow(
            onTabSelected = onTypeSelected,
            selectedTab = selectedType,
            scrollState = filterRowScrollState,
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
                    // 확정 문구(#567)가 두 줄이라 가운데 정렬 Box 안에서 줄맞춤을 위해 명시한다.
                    textAlign = TextAlign.Center,
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
