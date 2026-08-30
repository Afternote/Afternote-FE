package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val isRefreshing = refreshState is LoadState.Loading && items.itemCount > 0

    // 직전 렌더에 상단(헤더·카테고리 필터 행)이 있었는지. 카테고리 전환은 새 Paging 세대를 만들어
    // `refresh = Loading` + `itemCount == 0` 을 다시 만드는데, 그 상태만 보면 «첫 진입» 과 구분이 없어
    // 이미 보고 있던 상단까지 로딩 화면으로 덮인다 (#1635). 상단 유무를 되먹여 그 둘을 가른다.
    // 되먹임이 발산하지 않는 이유: 이 값은 로딩 갈래의 결과만 바꾸고, 그 갈래가 내는 두 상태
    // (InitialLoading·Reloading)의 상단 유무는 각각 false·true 로 자기 입력과 같다.
    // 화면 회전에도 유지해야 전환 도중 회전이 상단을 다시 걷어 가지 않는다.
    var chromeAlreadyVisible by rememberSaveable { mutableStateOf(false) }
    val bodyState =
        afternoteHomeBodyState(
            refreshState = refreshState,
            itemCount = items.itemCount,
            selectedType = selectedType,
            chromeAlreadyVisible = chromeAlreadyVisible,
        )
    val drawsTopChrome = bodyState.drawsTopChrome(showsHeaderOnEmptyList)
    LaunchedEffect(drawsTopChrome) { chromeAlreadyVisible = drawsTopChrome }

    // 카테고리 필터 행의 가로 스크롤 위치는 본문 분기 «위» 에서 만든다. 행 안에서 remember 하면 본문이
    // 바뀔 때마다 서브트리가 폐기돼 0 으로 돌아가고, 오른쪽으로 밀어 고른 끝 탭이 전환 후 화면 밖으로
    // 나간다. 호출부가 둘(목록·0건)이라 «같은 자리» 도 아니었다 (#1635).
    val filterRowScrollState = rememberScrollState()

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
            when (bodyState) {
                AfternoteHomeBodyState.InitialLoading -> {
                    LoadingBody(modifier = bodyModifier)
                }

                is AfternoteHomeBodyState.Reloading -> {
                    ReloadingBody(
                        headerDescription = headerDescription,
                        nextStep = nextStep,
                        selectedType = bodyState.selectedType,
                        onTypeSelected = onTypeSelected,
                        filterRowScrollState = filterRowScrollState,
                        modifier = bodyModifier,
                    )
                }

                AfternoteHomeBodyState.Error -> {
                    ErrorListBody(
                        onRetry = items::retry,
                        modifier = bodyModifier,
                    )
                }

                is AfternoteHomeBodyState.FilteredError -> {
                    FilteredErrorBody(
                        headerDescription = headerDescription,
                        nextStep = nextStep,
                        selectedType = bodyState.selectedType,
                        onTypeSelected = onTypeSelected,
                        onRetry = items::retry,
                        filterRowScrollState = filterRowScrollState,
                        modifier = bodyModifier,
                    )
                }

                AfternoteHomeBodyState.List -> {
                    InfiniteListBody(
                        modifier = bodyModifier,
                        nextStep = nextStep,
                        items = items,
                        selectedType = selectedType,
                        onTypeSelected = onTypeSelected,
                        onListItemClick = onListItemClick,
                        headerDescription = headerDescription,
                        filterRowScrollState = filterRowScrollState,
                    )
                }

                AfternoteHomeBodyState.Empty -> {
                    if (showsHeaderOnEmptyList) {
                        EmptyHomeBody(
                            headerDescription = headerDescription,
                            nextStep = nextStep,
                            emptyListDescription = emptyListDescription,
                            onTypeSelected = onTypeSelected,
                            filterRowScrollState = filterRowScrollState,
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
 * 상단(헤더·카테고리 필터 행)을 이미 보고 있던 사용자가 맞는 로딩 본문 (#1635).
 *
 * **[com.afternote.core.ui.loading.LoadingBody] 로 화면을 통째로 덮지 않는다.** 카테고리를 바꾸면
 * [AfternoteHomeViewModel] 의 `flatMapLatest` 가 새 Paging 세대를 만들고, 0건 상태에서 전환하면
 * `refresh = Loading` + `itemCount == 0` 이라 종전에는 첫 진입과 똑같이 판정돼 **방금 탭한 카테고리
 * 행까지 사라졌다가 다시 나타났다.** 사라진 동안 그 화면에는 조작할 것이 하나도 없고, 응답이 느리면
 * 그 상태가 그대로 길어진다. 어느 로딩이 이 본문을 타는지는 [afternoteHomeBodyState] 가 가른다.
 *
 * 상단 배치는 목록·필터 0건·필터 실패와 같다 — [HomeBodyTopSpacing]·[HomeBodySectionSpacing] 를 공유해
 * 로딩이 끼어도 헤더와 행이 **같은 자리에** 머문다. 위쪽이 뛰지 않는 것이 이 본문의 목적이다.
 *
 * 이 본문도 `showsHeaderOnEmptyList` 를 보지 않는다. 볼 필요가 없다 — [drawsTopChrome] 되먹임 때문에
 * 상단이 없던 상태에서 시작한 로드는 애초에 이 본문에 오지 않는다. 수신자 0건에서 카테고리를 고르면
 * 종전대로 전체 로딩을 지나 목록 상태(그쪽도 헤더를 그린다)로 간다.
 */
@Composable
internal fun ReloadingBody(
    headerDescription: String,
    nextStep: NextStep?,
    selectedType: AfternoteType?,
    onTypeSelected: (AfternoteType?) -> Unit,
    filterRowScrollState: ScrollState,
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
        // 필터 행과 로딩 사이에는 간격을 두지 않는다 — 목록 상태의 AfternoteListContent 와 같은 배치다.
        Column(modifier = Modifier.weight(1f)) {
            // 방금 탭한 카테고리를 로딩 중에도 선택 상태로 둔다. 「전체」로 돌아오는 전환이면 null 이다.
            AfternoteTypeFilterRow(
                onTabSelected = onTypeSelected,
                selectedTab = selectedType,
                scrollState = filterRowScrollState,
            )
            LoadingBody(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 카테고리 필터를 건 채 조회가 실패했을 때의 본문 (#1634).
 *
 * **전면 에러([ErrorListBody])로 덮지 않는다.** 그 본문에 남는 조작은 «다시 시도» 하나뿐이라 카테고리
 * 행이 사라지고, 서버가 계속 실패하는 동안 사용자는 자기가 고른 카테고리에 갇힌다 — 다른 카테고리로도
 * 「전체」로도 나갈 수 없는 막다른 상태다. 카테고리를 고르면 [AfternoteHomeViewModel.selectTab] 이
 * 새 조회를 걸므로, 행만 남겨 두면 그 자체가 복구 수단이 된다.
 *
 * **실패를 0건 문구로 덮지도 않는다.** 목록 경로의 필터 0건 문구(`afternote_home_filtered_empty`)는
 * «이 카테고리에 등록된 애프터노트가 없어요» 라고 단정하는데, 실제로는 서버 응답을 못 받은 것이고
 * 재시도 수단까지 사라진다 — 무음 실패(#705)와 같은 부류다. 그래서 카테고리 행만 목록 상태에서
 * 가져오고 그 아래는 실패 문구와 재시도를 그대로 둔다.
 *
 * 헤더를 함께 그리는 이유: 같은 필터의 성공 상태(목록·필터 0건)는
 * [com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody] 로 헤더를 그린다.
 * 실패에서만 상단이 통째로 사라지면 카테고리를 옮길 때마다 화면 위쪽이 뛴다. 여백을
 * [HomeBodyTopSpacing]·[HomeBodySectionSpacing] 로 공유해 세 상태가 헤더·필터 행을 같은 자리에 둔다.
 * 이 본문은 `showsHeaderOnEmptyList` 를 보지 않는다 — 그 값은 «0건이고 필터도 없을 때» 를 가르는 것이라
 * (KDoc 참조) 필터가 걸린 이 상태와 무관하고, 수신자도 필터가 걸린 성공 상태에서는 이미 헤더를 본다.
 */
@Composable
internal fun FilteredErrorBody(
    headerDescription: String,
    nextStep: NextStep?,
    selectedType: AfternoteType,
    onTypeSelected: (AfternoteType?) -> Unit,
    onRetry: () -> Unit,
    filterRowScrollState: ScrollState,
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
        // 필터 행과 실패 본문 사이에는 간격을 두지 않는다 — 목록 상태의 AfternoteListContent 와 같은 배치다.
        // 남은 높이를 weight 로 받아 실패 문구가 «필터 행 아래 영역» 의 가운데에 온다.
        Column(modifier = Modifier.weight(1f)) {
            AfternoteTypeFilterRow(
                onTabSelected = onTypeSelected,
                selectedTab = selectedType,
                scrollState = filterRowScrollState,
            )
            ErrorListBody(
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
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
    filterRowScrollState: ScrollState,
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
                scrollState = filterRowScrollState,
            )
            EmptyListBody(description = emptyListDescription)
        }
    }
}
