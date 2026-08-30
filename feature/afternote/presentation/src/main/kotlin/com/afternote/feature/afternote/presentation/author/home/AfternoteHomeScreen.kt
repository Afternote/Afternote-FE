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
 * @param showsHeaderOnEmptyList 목록이 0건이고 카테고리 필터도 없을 때 헤더(제목·설명·NEXT STEP 슬롯)를
 *   그릴지. 작성자 시안(`애프터노트_목록X` 4327:66762)은 0건에서도 헤더를 그대로 두므로 작성자는 `true` 다.
 *   수신자 빈 상태 시안은 확인된 바 없어 종전 렌더(헤더 없음)를 유지한다 — 기본값을 두지 않는 이유는
 *   [HomeHeaderSection] KDoc 과 같다(#620·#777): 관점이 갈리는 조각은 호출부가 매번 명시한다 (#1175).
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
                            modifier = bodyModifier,
                        )
                    } else {
                        EmptyListBody(modifier = bodyModifier)
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
 */
@Composable
internal fun EmptyHomeBody(
    headerDescription: String,
    nextStep: NextStep?,
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
        EmptyListBody()
    }
}
