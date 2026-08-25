package com.afternote.feature.receiver.presentation.afternotelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeScreen
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.asDisplayText
import com.afternote.core.ui.R as CoreUiR

/**
 * 수신자 애프터노트 목록 Entry.
 *
 * 작성자 측과 동일한 Paging 3 스트림 + [AfternoteHomeScreen]을 그대로 재사용한다. 단 처리가
 * 갈리는 실패([ReceiverAfternoteListError])는 그 화면에 태우지 않는다 — 작성자 화면의 전면 에러는
 * «다시 시도» 하나뿐이라, 재시도가 무의미한 «전달 조건 미충족» 과 안내가 달라야 하는 «연결 없음» 을
 * 모두 같은 문구로 수렴시킨다(#611).
 */
@Composable
fun ReceiverAfternoteHomeEntry(
    navigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiverAfternoteHomeViewModel = hiltViewModel(),
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val items = viewModel.pagedAfternotes.collectAsLazyPagingItems()
    // 이미 그려 둔 목록이 있으면 유지한다 — 전면 교체는 보여 줄 것이 전무할 때만 (작성자 화면과 같은 규칙).
    val listError =
        (items.loadState.refresh as? LoadState.Error)
            ?.error
            ?.toListError(R.string.receiver_afternote_list_not_deliverable_message)
            ?.takeIf { items.itemCount == 0 }

    when (listError) {
        is ReceiverAfternoteListError.NotDeliverable -> {
            ReceiverAfternoteListErrorBody(
                title = listError.payload.asDisplayText(),
                description = stringResource(R.string.receiver_afternote_list_not_deliverable_description),
                modifier = modifier,
            )
        }

        ReceiverAfternoteListError.NetworkUnavailable -> {
            ReceiverAfternoteListErrorBody(
                title = stringResource(CoreUiR.string.core_ui_network_error_title),
                description = stringResource(CoreUiR.string.core_ui_network_error_description),
                modifier = modifier,
                retry =
                    ListErrorRetry(
                        label = stringResource(CoreUiR.string.core_ui_network_error_retry),
                        onClick = items::retry,
                    ),
            )
        }

        null -> {
            AfternoteHomeScreen(
                items = items,
                selectedCategory = selectedTab,
                onCategorySelected = viewModel::selectTab,
                onListItemClick = { id, _ -> navigateToDetail(id) },
                modifier = modifier,
            )
        }
    }
}
