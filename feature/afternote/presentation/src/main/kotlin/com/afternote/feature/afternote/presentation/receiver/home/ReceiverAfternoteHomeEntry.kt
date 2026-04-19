package com.afternote.feature.afternote.presentation.receiver.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeScreen

data class ReceiverAfternoteHomeEntryActions(
    val navigateToDetail: (String) -> Unit = {},
    val onNavTabSelected: (BottomNavTab) -> Unit = {},
)

/**
 * 수신자 애프터노트 목록 Entry.
 *
 * ViewModel에서 데이터를 로드·가공하고, 공용 [AfternoteHomeScreen]에 전달합니다.
 */
@Composable
fun ReceiverAfternoteHomeEntry(
    modifier: Modifier = Modifier,
    viewModel: ReceiverAfternoteHomeViewModel = hiltViewModel(),
    actions: ReceiverAfternoteHomeEntryActions = ReceiverAfternoteHomeEntryActions(),
) {
    val bodyUiState by viewModel.bodyUiState.collectAsStateWithLifecycle()

    AfternoteHomeScreen(
        listState = bodyUiState,
        onCategorySelected = { viewModel.onEvent(ReceiverAfternoteHomeEvent.SelectTab(it)) },
        onListItemClick = actions.navigateToDetail,
        modifier = modifier,
    )
}
