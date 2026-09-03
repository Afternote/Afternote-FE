package com.afternote.feature.afternote.presentation.draft

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.component.AfternoteList
import com.afternote.feature.afternote.presentation.shared.component.EmptyListBody
import com.afternote.feature.afternote.presentation.shared.component.ErrorListBody
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel

/**
 * 임시저장 목록 화면 — 항목을 고르면 상세가 아니라 **에디터로 이어쓴다**.
 *
 * 시안이 없어(애프터노트 구역에 임시저장 화면 0건, 2026-09-03 전량 대조) 같은 제품의 기존 임시저장
 * 화면(타임레터 「임시 저장된 레터」)을 따라 세웠다 — 뒤로가기 + 제목, 카드 목록.
 *
 * 「총 N 개」는 넣지 않았다. 목록 응답이 `content/page/size/hasNext` 뿐이라 총계가 없고
 * (BE `AfternotePageResponse`), 지금까지 불러온 개수를 총계처럼 적으면 스크롤에 따라 숫자가 자란다.
 */
@Composable
fun AfternoteDraftListScreen(
    items: LazyPagingItems<ListItemUiModel>,
    onBackClick: () -> Unit,
    onDraftClick: (id: Long, type: AfternoteType) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val refreshState = items.loadState.refresh
    val isInitialLoading = refreshState is LoadState.Loading && items.itemCount == 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.afternote_draft_list_title),
                onBackClick = onBackClick,
            )
        },
    ) { paddingValues ->
        val bodyModifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        when {
            isInitialLoading -> {
                LoadingBody(modifier = bodyModifier)
            }

            refreshState is LoadState.Error -> {
                ErrorListBody(onRetry = items::retry, modifier = bodyModifier)
            }

            items.itemCount == 0 -> {
                EmptyListBody(
                    description = stringResource(R.string.afternote_draft_list_empty_body),
                    modifier = bodyModifier,
                )
            }

            else -> {
                AfternoteList(
                    items = items,
                    onItemClick = onDraftClick,
                    modifier = bodyModifier,
                )
            }
        }
    }
}
