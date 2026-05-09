package com.afternote.feature.afternote.presentation.receiver.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeScreen

data class ReceiverAfternoteHomeEntryActions(
    val navigateToDetail: (String) -> Unit = {},
    val onNavTabSelected: (BottomNavTab) -> Unit = {},
)

/**
 * 수신자 애프터노트 목록 Entry.
 *
 * 단일 호출 결과를 [androidx.paging.PagingData.from]으로 감싸 작성자 화면과 동일한
 * [AfternoteHomeScreen]을 재사용한다. 정적 PagingData는 LoadState로 로딩을 표현하지
 * 못하므로 초기 로딩 여부는 ViewModel uiState에서 직접 계산해 전달한다.
 */
@Composable
fun ReceiverAfternoteHomeEntry(
    modifier: Modifier = Modifier,
    viewModel: ReceiverAfternoteHomeViewModel = hiltViewModel(),
    actions: ReceiverAfternoteHomeEntryActions = ReceiverAfternoteHomeEntryActions(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = viewModel.pagedAfternotes.collectAsLazyPagingItems()

    AfternoteHomeScreen(
        items = items,
        selectedCategory = uiState.selectedTab,
        isInitialLoading = uiState.isLoading && items.itemCount == 0,
        isRefreshing = false,
        onCategorySelected = { viewModel.onEvent(ReceiverAfternoteHomeEvent.SelectTab(it)) },
        onListItemClick = { id, _ -> actions.navigateToDetail(id) },
        onRefresh = {},
        modifier = modifier,
    )
}
