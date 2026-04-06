package com.afternote.feature.afternote.presentation.receiver.navgraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeScreen
import com.afternote.feature.afternote.presentation.receiver.model.ReceiverAfternoteHomeEvent
import com.afternote.feature.afternote.presentation.receiver.viewmodel.ReceiverAfternoteHomeViewModel
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState

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
    viewModel: ReceiverAfternoteHomeViewModel = hiltViewModel(),
    actions: ReceiverAfternoteHomeEntryActions = ReceiverAfternoteHomeEntryActions(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AfternoteHomeScreen(
        listState =
            AfternoteBodyUiState(
                visibleItems = uiState.visibleItems,
                selectedCategory = uiState.selectedTab,
            ),
        onNavTabSelected = actions.onNavTabSelected,
        onCategorySelected = { viewModel.onEvent(ReceiverAfternoteHomeEvent.SelectTab(it)) },
        onListItemClick = actions.navigateToDetail,
        modifier = modifier,
        selectedNavTab = uiState.selectedBottomNavItem,
    )
}
