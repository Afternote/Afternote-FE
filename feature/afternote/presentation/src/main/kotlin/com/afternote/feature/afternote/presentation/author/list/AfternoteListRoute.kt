package com.afternote.feature.afternote.presentation.author.list

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.presentation.author.list.model.AfternoteListEvent
import com.afternote.feature.afternote.presentation.author.list.screen.AfternoteListScreen
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

// TODO: AI 딸깍이라 점검 필요
data class AfternoteListRouteActions(
    val navigateToDetail: (String) -> Unit = {},
    val navigateToGalleryDetail: (String) -> Unit = {},
    val navigateToMemorialGuidelineDetail: (String) -> Unit = {},
    val navigateToAdd: (AfternoteCategory) -> Unit = {},
    val onNavTabSelected: (BottomNavTab) -> Unit = {},
)

/**
 * 애프터노트 목록 Route.
 *
 * ViewModel에서 데이터를 로드·가공하고, Route는 Screen에 전달만 합니다.
 */
@Composable
fun AfternoteListRoute(
    viewModel: AfternoteListViewModel = hiltViewModel(),
    actions: AfternoteListRouteActions = AfternoteListRouteActions(),
    onItemsChanged: (List<Item>) -> Unit = {},
    listRefreshRequested: Boolean = false,
    onListRefreshConsumed: () -> Unit = {},
) {
    LaunchedEffect(listRefreshRequested) {
        if (listRefreshRequested) {
            viewModel.loadAfternotes()
            onListRefreshConsumed()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bodyUiState by viewModel.bodyUiState.collectAsStateWithLifecycle()

    // 상위로 items 전파 (Edit 화면에서 사용)
    val itemIds = remember(uiState.items) { uiState.items.map { it.id }.toSet() }
    LaunchedEffect(itemIds) {
        if (uiState.items.isNotEmpty()) {
            Log.d("AfternoteListRoute", "items changed: size=${uiState.items.size}")
            onItemsChanged(uiState.items)
        }
    }

    AfternoteListScreen(
        listState = bodyUiState,
        onNavTabSelected = actions.onNavTabSelected,
        onCategorySelected = { viewModel.onEvent(AfternoteListEvent.SelectTab(it)) },
        onListItemClick = actions.navigateToDetail,
        selectedNavTab = uiState.selectedBottomNavItem,
        onLoadMore = viewModel::loadNextPage,
    ) { actions.navigateToAdd(uiState.selectedTab) }
}
