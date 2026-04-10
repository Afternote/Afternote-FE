package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.home.model.AfternoteHomeEvent
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

data class AfternoteHomeEntryActions(
    val navigateToDetail: (String) -> Unit = {},
    val navigateToGalleryDetail: (String) -> Unit = {},
    val navigateToMemorialGuidelineDetail: (String) -> Unit = {},
    val navigateToAdd: (AfternoteCategory) -> Unit = {},
    val onNavTabSelected: (BottomNavTab) -> Unit = {},
)

/**
 * 애프터노트 목록 Entry.
 *
 * ViewModel에서 데이터를 로드·가공하고, Entry는 Screen에 전달만 합니다.
 */
@Composable
fun AfternoteHomeEntry(
    viewModel: AfternoteHomeViewModel = hiltViewModel(),
    actions: AfternoteHomeEntryActions = AfternoteHomeEntryActions(),
) {
    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle() // 내부의 collect로 viewModel.uiState를 관찰/수집
    val bodyUiState by viewModel
        .bodyUiState
        .collectAsStateWithLifecycle()

    AfternoteHomeScreen(
        listState = bodyUiState,
        onCategorySelected = { viewModel.onEvent(AfternoteHomeEvent.SelectTab(it)) },
        onListItemClick = actions.navigateToDetail,
        onLoadMore = { viewModel.onEvent(AfternoteHomeEvent.LoadMore) },
    ) { actions.navigateToAdd(uiState.categoryState.selectedCategory) }
}
