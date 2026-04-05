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
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import kotlinx.coroutines.flow.Flow

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
    // 현재 화면에 보이는 items를 상위로 브로드캐스팅하는 Output 콜백.
    // 구독자(예: HostViewModel)가 관심 없으면 무시해도 컴포넌트 기능은 온전히 동작하므로 옵셔널.
    onVisibleItemsUpdated: (List<ListItem>) -> Unit = {},
    // 상위(graph-scoped HostViewModel)에서 발행하는 one-shot 새로고침 이벤트 스트림.
    // 컴포넌트가 동작하기 위해 수신해야 하는 Input 의존성이므로 디폴트 없이 강제
    // (누락 시 에디터 저장·상세 삭제 후 홈이 새로고침되지 않는 silent failure 방지).
    homeRefreshEvents: Flow<Unit>,
) {
    LaunchedEffect(homeRefreshEvents) {
        homeRefreshEvents.collect {
            viewModel.onEvent(AfternoteHomeEvent.Refresh)
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
