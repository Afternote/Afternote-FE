package com.afternote.feature.receiver.presentation.afternotelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeScreen
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.asDisplayText
import com.afternote.feature.receiver.presentation.error.isDeliveryConditionNotMet
import com.afternote.feature.receiver.presentation.error.toErrorPayload

/**
 * 수신자 애프터노트 목록 Entry.
 *
 * 작성자 측과 동일한 Paging 3 스트림 + [AfternoteHomeScreen]을 그대로 재사용한다. 단 «전달 조건
 * 미충족» 만은 그 화면에 태우지 않는다 — 작성자 화면의 전면 에러는 재시도를 전제하는데, 이 실패는
 * 발신자가 세운 조건이 충족돼야 풀려 재시도가 무의미하기 때문이다(#611).
 */
@Composable
fun ReceiverAfternoteHomeEntry(
    navigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiverAfternoteHomeViewModel = hiltViewModel(),
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val items = viewModel.pagedAfternotes.collectAsLazyPagingItems()
    val notDeliverable =
        (items.loadState.refresh as? LoadState.Error)
            ?.error
            ?.takeIf { it.isDeliveryConditionNotMet() }

    // 이미 그려 둔 목록이 있으면 유지한다 — 전면 교체는 보여줄 것이 전무할 때만 (작성자 화면과 같은 규칙).
    if (notDeliverable != null && items.itemCount == 0) {
        ReceiverAfternoteNotDeliverableBody(
            message =
                notDeliverable
                    .toErrorPayload(R.string.receiver_afternote_list_not_deliverable_message)
                    .asDisplayText(),
            modifier = modifier,
        )
    } else {
        AfternoteHomeScreen(
            items = items,
            selectedCategory = selectedTab,
            onCategorySelected = viewModel::selectTab,
            onListItemClick = { id, _ -> navigateToDetail(id) },
            modifier = modifier,
        )
    }
}
