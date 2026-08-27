package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.button.FAB.PenFloatingActionButton
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.body.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.body.ErrorListBody
import com.afternote.feature.afternote.presentation.shared.body.LoadingListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf

/**
 * 애프터노트 목록 화면. 작성자(발신자)와 수신자가 같은 목록·카드·필터를 쓰므로 한 화면을 공유하고,
 * 관점이 갈리는 조각만 호출부가 채운다.
 *
 * @param headerDescription 상단 헤더 한 줄. 기본값을 두지 않는 이유는 [HomeHeaderSection] KDoc 참조 (#620).
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
                    LoadingListBody(modifier = bodyModifier)
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
                    EmptyListBody(modifier = bodyModifier)
                }
            }
        }
    }
}

// 작성자(author) 플로우: onFabClick 을 넘겨 48dp Pen FAB(#481)를 프리뷰에 실제로 렌더한다.
@Preview(showBackground = true, backgroundColor = 0xFFFAFAFA)
@Composable
private fun AfternoteHomeScreenPreview() {
    AfternoteTheme {
        val items =
            flowOf(
                PagingData.from(
                    listOf(
                        ListItemUiModel(
                            id = 1L,
                            serviceName = "인스타그램",
                            date = "2023.11.24",
                            iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteType.SOCIAL_NETWORK,
                        ),
                        ListItemUiModel(
                            id = 2L,
                            serviceName = "페이스북",
                            date = "2023.11.25",
                            iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteType.SOCIAL_NETWORK,
                        ),
                    ),
                ),
            ).collectAsLazyPagingItems()
        AfternoteHomeScreen(
            items = items,
            selectedType = null,
            onTypeSelected = {},
            onListItemClick = { _, _ -> },
            headerDescription = stringResource(R.string.afternote_home_header_description),
            nextStep =
                NextStep(
                    text = "가족들의 '주거래 은행' 정보를\n입력하신 건 확인하셨나요?",
                    onClick = {},
                ),
            onFabClick = {},
            onSettingClick = {},
        )
    }
}

// 초기 로딩 상태(refresh=Loading, 0건) → LoadingListBody. Paging 의 loadState 를 주입해 재현한다.
@Preview(showBackground = true)
@Composable
private fun AfternoteHomeScreenLoadingPreview() {
    AfternoteTheme {
        val items =
            flowOf(
                PagingData.empty<ListItemUiModel>(
                    LoadStates(
                        refresh = LoadState.Loading,
                        prepend = LoadState.NotLoading(endOfPaginationReached = false),
                        append = LoadState.NotLoading(endOfPaginationReached = false),
                    ),
                ),
            ).collectAsLazyPagingItems()
        AfternoteHomeScreen(
            items = items,
            selectedType = null,
            onTypeSelected = {},
            onListItemClick = { _, _ -> },
            headerDescription = stringResource(R.string.afternote_home_header_description),
            nextStep =
                NextStep(
                    text = "가족들의 '주거래 은행' 정보를\n입력하신 건 확인하셨나요?",
                    onClick = {},
                ),
            onFabClick = {},
            onSettingClick = {},
        )
    }
}
