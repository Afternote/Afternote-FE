package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.afternote.core.ui.button.FAB.PenFloatingActionButton
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.body.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.body.ErrorListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel

/**
 * 애프터노트 목록 화면. 작성자(발신자)와 수신자가 같은 목록·카드·필터를 쓰므로 한 화면을 공유하고,
 * 관점이 갈리는 조각만 호출부가 채운다.
 *
 * @param headerDescription 상단 헤더 한 줄. 기본값을 두지 않는 이유는 [HomeHeaderSection] KDoc 참조 (#620).
 * @param showsHeaderOnEmptyList 목록이 0건이고 카테고리 필터도 없을 때 화면 상단(제목·설명·NEXT STEP
 *   슬롯 **과 카테고리 필터 행**)을 그릴지. 작성자 시안(`애프터노트_목록X` 4327:66762)은 0건에서도 이
 *   상단을 그대로 두므로 작성자는 `true` 다.
 *   수신자는 `false` — 시안이 «상단 없음» 이어서가 아니라 **수신자 0건 시안이 아예 없어서**다. 2026-08-31
 *   에 정본 페이지(`4327:43064`) TEXT 노드 6482개를 전수 조회했고, 수신자 구역(4327:73596)의 빈 상태는
 *   「받은 기록함」(4327:74361) 하나뿐이다 — 그건 + FAB 을 가진 다른 화면이라 이 목록의 근거가 못 된다.
 *   기본값을 두지 않는 이유는 [HomeHeaderSection] KDoc 과 같다(#620·#777): 관점이 갈리는 조각은 호출부가
 *   매번 명시한다 (#1175).
 * @param emptyListDescription 0건 본문([EmptyListBody])의 안내 문구. 기본값을 두지 않는 이유는
 *   [EmptyListBody] KDoc 참조 — 발신자 문구는 «아래 연필 버튼을 눌러» 로 끝나는데 수신자에게는 그 FAB 이
 *   없다 (#620).
 * @param onSettingClick 설정 진입. `null`(기본)이면 탑바의 회원 액션(프로필·설정)을 그리지 않는다 —
 *   수신자는 로그인 사용자가 아니라 두 아이콘 모두 향할 곳이 없다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfternoteHomeScreen(
    items: LazyPagingItems<ListItemUiModel>,
    selectedType: AfternoteType?,
    onTypeSelected: (AfternoteType?) -> Unit,
    onListItemClick: (id: Long, type: AfternoteType) -> Unit,
    headerDescription: String,
    nextStep: NextStep?,
    showsHeaderOnEmptyList: Boolean,
    emptyListDescription: String,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onFabClick: (() -> Unit)? = null,
    onSettingClick: (() -> Unit)? = null,
) {
    val refreshState = items.loadState.refresh
    val isInitialLoading = refreshState is LoadState.Loading && items.itemCount == 0
    val isRefreshing = refreshState is LoadState.Loading && items.itemCount > 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeTopBar(
                showProfileIcon = onSettingClick != null,
                onSettingClick = onSettingClick,
            )
        },
        floatingActionButton = {
            if (onFabClick != null) {
                // 시안(plus_button 48×48) 정합: core/ui 기본 56dp 대신 48dp opt-in (#481).
                PenFloatingActionButton(onClick = onFabClick, size = 48.dp, iconSize = 17.dp)
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = items::refresh,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            val bodyModifier = Modifier.fillMaxSize()
            when {
                isInitialLoading -> {
                    LoadingBody(modifier = bodyModifier)
                }

                // 전면 에러는 보여줄 데이터가 전무할 때만. 목록이 있는 상태의 refresh 실패는
                // Paging 이 기존 페이지를 유지하므로(itemCount > 0) 아래 분기가 목록을 그대로 보여준다.
                refreshState is LoadState.Error && items.itemCount == 0 -> {
                    ErrorListBody(
                        onRetry = items::retry,
                        modifier = bodyModifier,
                    )
                }

                // 카테고리 필터 0건도 이 경로에 남겨 카테고리 행을 유지한다(막다른 상태 방지).
                items.itemCount > 0 || selectedType != null -> {
                    InfiniteListBody(
                        modifier = bodyModifier,
                        nextStep = nextStep,
                        items = items,
                        selectedType = selectedType,
                        onTypeSelected = onTypeSelected,
                        onListItemClick = onListItemClick,
                        headerDescription = headerDescription,
                    )
                }

                else -> {
                    if (showsHeaderOnEmptyList) {
                        EmptyHomeBody(
                            headerDescription = headerDescription,
                            nextStep = nextStep,
                            emptyListDescription = emptyListDescription,
                            onTypeSelected = onTypeSelected,
                            modifier = bodyModifier,
                        )
                    } else {
                        EmptyListBody(description = emptyListDescription, modifier = bodyModifier)
                    }
                }
            }
        }
    }
}

/**
 * 애프터노트가 0건이고 카테고리 필터도 없는 «첫 진입» 본문.
 *
 * 종전에는 헤더가 [com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody]
 * 안에만 있어, 이 경로로 빠지는 사용자에게 화면 제목(`afternote_home_title`)과 설명이 **한 번도 그려지지
 * 않았다.** 서버 데이터와 무관한 구조 결함이라 목록 상태와 같은 자리에 헤더를 올려 닫는다 (#1175).
 *
 * 여백을 [HomeBodyTopSpacing]·[HomeBodySectionSpacing] 로 공유해 목록 상태와 헤더 위치가 어긋나지 않게 한다.
 *
 * [nextStep] 은 여전히 호출부가 정한다 — 문구를 만드는 원천이 서버에도 ViewModel 에도 없어 현재는 `null`
 * 이고(Afternote-BE#270), 이 함수는 값이 생겼을 때 카드가 헤더 아래 제자리에 붙는 것만 보장한다.
 *
 * 카테고리 필터 행도 여기서 그린다 — 시안 `애프터노트_목록X` 는 0건에서도 헤더 아래 필터 행을 두고,
 * 그게 없으면 첫 진입 사용자는 카테고리라는 축이 있다는 것 자체를 볼 수 없다. **이 경로를 목록 상태의
 * [com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody] 로 합치지 마라** —
 * 그쪽 [com.afternote.feature.afternote.presentation.shared.body.infinite.content.AfternoteListContent] 의
 * 0건 문구는 `afternote_home_filtered_empty`(카테고리 필터 결과 0건)라, 합치는 순간 전체 0건 문구
 * `feature_afternote_empty_list_body` 가 그 문구로 뒤바뀐다. 두 문구는 #567 에서 일부러 갈라 놓은 것이다.
 */
@Composable
internal fun EmptyHomeBody(
    headerDescription: String,
    nextStep: NextStep?,
    emptyListDescription: String,
    onTypeSelected: (AfternoteType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HomeBodySectionSpacing),
    ) {
        Spacer(Modifier.height(HomeBodyTopSpacing))
        HomeHeaderSection(
            description = headerDescription,
            nextStep = nextStep,
        )
        // 필터 행과 빈 본문 사이에는 간격을 두지 않는다 — 목록 상태의 AfternoteListContent 와 같은 배치다.
        Column {
            // 이 본문은 «0건이고 필터도 없음» 분기에서만 그려지므로 선택 탭은 정의상 «전체»(null)다.
            // selectedType 을 넘겨받지 않는 이유이자, `무필터 0건 본문은 전체 탭을 선택 상태로 그린다`
            // 테스트가 이 불변을 고정한다.
            AfternoteTypeFilterRow(
                onTabSelected = onTypeSelected,
                selectedTab = null,
            )
            EmptyListBody(description = emptyListDescription)
        }
    }
}
