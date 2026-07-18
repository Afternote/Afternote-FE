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
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.core.ui.button.FAB.PenFloatingActionButton
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.body.ErrorListBody
import com.afternote.feature.afternote.presentation.shared.body.LoadingListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.InfiniteListBody
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfternoteHomeScreen(
    items: LazyPagingItems<ListItemUiModel>,
    selectedCategory: AfternoteCategory,
    onCategorySelected: (AfternoteCategory) -> Unit,
    onListItemClick: (id: String, type: AfternoteServiceType) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onFabClick: (() -> Unit)? = null,
    onSettingClick: () -> Unit = {},
) {
    val refreshState = items.loadState.refresh
    val isInitialLoading = refreshState is LoadState.Loading && items.itemCount == 0
    val isRefreshing = refreshState is LoadState.Loading && items.itemCount > 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeTopBar(onSettingClick = onSettingClick)
        },
        floatingActionButton = {
            if (onFabClick != null) {
                PenFloatingActionButton(onClick = onFabClick)
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
                items.itemCount > 0 || selectedCategory != AfternoteCategory.ALL -> {
                    InfiniteListBody(
                        modifier = bodyModifier,
                        items = items,
                        selectedCategory = selectedCategory,
                        onCategorySelected = onCategorySelected,
                        onListItemClick = onListItemClick,
                    )
                }

                else -> {
                    EmptyListBody(modifier = bodyModifier)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAFA)
@Composable
private fun AfternoteHomeScreenPreview() {
    AfternoteTheme {
        val items =
            flowOf(
                PagingData.from(
                    listOf(
                        ListItemUiModel(
                            id = "1",
                            serviceName = "인스타그램",
                            date = "2023.11.24",
                            iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteServiceType.SOCIAL_NETWORK,
                        ),
                        ListItemUiModel(
                            id = "2",
                            serviceName = "페이스북",
                            date = "2023.11.25",
                            iconResId = R.drawable.feature_afternote_img_insta_pattern,
                            type = AfternoteServiceType.SOCIAL_NETWORK,
                        ),
                    ),
                ),
            ).collectAsLazyPagingItems()
        AfternoteHomeScreen(
            items = items,
            selectedCategory = AfternoteCategory.ALL,
            onCategorySelected = {},
            onListItemClick = { _, _ -> },
        )
    }
}
