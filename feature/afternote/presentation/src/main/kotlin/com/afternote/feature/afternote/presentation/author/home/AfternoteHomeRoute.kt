package com.afternote.feature.afternote.presentation.author.home

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.home.model.AfternoteHomeEvent
import com.afternote.feature.afternote.presentation.author.home.screen.AfternoteHomeScreen
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

// TODO: AI 딸깍이라 점검 필요
data class AfternoteHomeRouteActions(
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
fun AfternoteHomeRoute(
    viewModel: AfternoteHomeViewModel = hiltViewModel(),
    actions: AfternoteHomeRouteActions = AfternoteHomeRouteActions(),
    onListItemsUpdated: (List<ListItem>) -> Unit = {},
    homeRefreshRequested: Boolean = false,
    onHomeRefreshConsumed: () -> Unit = {},
) {
    LaunchedEffect(homeRefreshRequested) {
        if (homeRefreshRequested) {
            viewModel.onEvent(AfternoteHomeEvent.Refresh)
            onHomeRefreshConsumed()
        }
    }

    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle() // 관찰 시작
    val bodyUiState by viewModel
        .bodyUiState
        .collectAsStateWithLifecycle()

    // 상위로 listItems 전파 (Editor 화면에서 사용)
    val listItems = uiState.listState.listItems
    val listItemIds =
        remember(listItems) {
            listItems
                .map { it.id }
                .toSet()
        }
    LaunchedEffect(listItemIds) {
        if (listItems.isNotEmpty()) {
            Log.d("AfternoteHomeRoute", "listItems changed: size=${listItems.size}")
            onListItemsUpdated(listItems)
        }
    }

    AfternoteHomeScreen(
        listState = bodyUiState,
        onNavTabSelected = actions.onNavTabSelected,
        onCategorySelected = { viewModel.onEvent(AfternoteHomeEvent.SelectTab(it)) },
        onListItemClick = actions.navigateToDetail,
        selectedNavTab = uiState.navState.selectedBottomNavItem,
        onLoadMore = { viewModel.onEvent(AfternoteHomeEvent.LoadMore) },
    ) { actions.navigateToAdd(uiState.categoryState.selectedCategory) }
}
