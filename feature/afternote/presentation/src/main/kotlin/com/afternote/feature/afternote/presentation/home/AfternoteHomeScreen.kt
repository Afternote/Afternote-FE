package com.afternote.feature.afternote.presentation.home

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
import com.afternote.feature.afternote.presentation.shared.component.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.component.ErrorListBody
import com.afternote.feature.afternote.presentation.shared.component.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.component.ListRefreshErrorBanner

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
    val bodyState =
        afternoteHomeBodyState(
            refreshState = refreshState,
            itemCount = items.itemCount,
            selectedType = selectedType,
        )

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
                        modifier = bodyModifier,
                    )
                }

                AfternoteHomeBodyState.List -> {
                    Column(modifier = bodyModifier) {
                        // 목록은 살아 있고 새로고침만 실패한 상태. 종전에는 이 갈래가 실패를 통째로
                        // 삼켜 «당겨도 아무 일도 없는» 화면이 됐다 (#705).
                        if (shouldShowRefreshErrorBanner(refreshState, items.itemCount)) {
                            ListRefreshErrorBanner(onRetry = items::retry)
                        }
                        InfiniteListBody(
                            // 배너가 붙으면 목록은 남은 높이를 채운다 — fillMaxSize 로 두면 배너 높이만큼 넘친다.
                            modifier = Modifier.weight(1f),
                            nextStep = nextStep,
                            items = items,
                            selectedType = selectedType,
                            onTypeSelected = onTypeSelected,
                            onListItemClick = onListItemClick,
                            headerDescription = headerDescription,
                        )
                    }
                }

                AfternoteHomeBodyState.Empty -> {
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
 * 목록을 유지한 채 새로고침 실패만 알려야 하는 상태인지 (#705).
 *
 * 보여 줄 것이 전무하면([itemCount] 0) 전면 오류([ErrorListBody])가 맡으므로 배너는 그리지 않는다 —
 * 두 표시가 겹치면 같은 실패를 두 번 말하게 된다.
 *
 * 이 파일 안에서만 쓰므로 private 다. 테스트가 부르려고 넓히지 않는다(#1678) — 세 갈래는
 * [AfternoteHomeScreen] 을 그려서 확인한다.
 */
private fun shouldShowRefreshErrorBanner(
    refreshState: LoadState,
    itemCount: Int,
): Boolean = refreshState is LoadState.Error && itemCount > 0
