package com.afternote.feature.afternote.presentation.receiver.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeScreen

/**
 * 수신자 애프터노트 목록 Entry.
 *
 * 작성자 측과 동일한 Paging 3 스트림 + [AfternoteHomeScreen]을 그대로 재사용한다.
 */
@Composable
fun ReceiverAfternoteHomeEntry(
    navigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiverAfternoteHomeViewModel = hiltViewModel(),
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val items = viewModel.pagedAfternotes.collectAsLazyPagingItems()

    AfternoteHomeScreen(
        items = items,
        selectedType = selectedTab,
        onTypeSelected = viewModel::selectTab,
        onListItemClick = { id, _ -> navigateToDetail(id) },
        modifier = modifier,
    )
}
