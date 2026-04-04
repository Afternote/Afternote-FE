package com.afternote.feature.afternote.presentation.author.home

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.author.home.model.AfternoteHomeEvent
import com.afternote.feature.afternote.presentation.author.home.screen.AfternoteHomeScreen
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
    onVisibleItemsUpdated: (List<ListItem>) -> Unit = {},
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

    // 상위로 visibleItems 전파 (Editor 화면에서 사용)
    // ListItem이 data class이므로 List.equals()가 구조적 비교를 수행하여
    // 리스트 내용이 실제로 바뀌었을 때만 LaunchedEffect가 재실행됩니다.
    val visibleItems = uiState.listState.visibleItems
    LaunchedEffect(visibleItems) {
        if (visibleItems.isNotEmpty()) {
            Log.d("AfternoteHomeEntry", "visibleItems changed: size=${visibleItems.size}")
            onVisibleItemsUpdated(visibleItems)
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
