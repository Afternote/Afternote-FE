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
    // 구독자(예: HostViewModel)가 관심 없으면 무시해도 컴포넌트 기능은 온전히 동작하므로 옵셔널.
    onVisibleItemsUpdated: (List<ListItem>) -> Unit = {},
    // 상위(graph-scoped HostViewModel)에서 발행하는 one-shot 새로고침 이벤트 스트림.
    // 컴포넌트가 동작하기 위해 수신해야 하는 Input 의존성이므로 디폴트 없이 강제
    // (누락 시 에디터 저장·상세 삭제 후 홈이 새로고침되지 않는 silent failure 방지).
    homeRefreshEvents: Flow<Unit>,
) {
    LaunchedEffect(homeRefreshEvents) {
        // homeRefreshEvents는 서브 그래프 Route.Afternote에 바인딩된 뷰모델의 프로퍼티
        // 서브 그래프는 팝될 일이 잘 없기 때문에 homeRefreshEvents도 변경될 일이 거의 없음
        // 그럼에도 LaunchedEffect가 의존하는 객체를 키로 설정하는 것이 권장되는 패턴
        homeRefreshEvents.collect {
            // 관찰/수집 시작
            // Unit 타입 데이터를 발행 받을 때마다 실행
            // 이 컴포저블이 호출되기 전에 신호가 도착했더라도 버퍼링되어 있기 때문에 버퍼에서 꺼내어 수집 가능
            // 실행 후에 데이터를 새로 발행받을 때까지 suspend
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
