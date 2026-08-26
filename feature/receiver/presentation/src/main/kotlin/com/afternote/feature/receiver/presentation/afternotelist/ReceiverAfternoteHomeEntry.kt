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
import com.afternote.core.ui.R as CoreUiR

/**
 * 수신자 애프터노트 목록 Entry.
 *
 * 작성자 측과 동일한 Paging 3 스트림 + [AfternoteHomeScreen]을 그대로 재사용한다. 단 처리가
 * 갈리는 실패([ReceiverAfternoteListError])는 그 화면에 태우지 않는다 — 작성자 화면의 전면 에러는
 * «다시 시도» 하나뿐이라, 재시도가 무의미한 «전달 조건 미충족» 과 안내가 달라야 하는 «연결 없음» 을
 * 모두 같은 문구로 수렴시킨다(#611).
 *
 * 화면을 공유해도 관점은 다르므로 발신자용 조각은 넘기지 않는다 (#620):
 * - 헤더 문구는 수신자 관점으로 덮는다 (기본값이 없어 안 넘기면 컴파일이 막는다).
 * - NEXT STEP 카드는 넘기지 않는다 (#777 이 같은 이유로 디폴트를 걷었다).
 * - `onSettingClick`·`onFabClick` 을 넘기지 않아 회원 액션(프로필·설정)과 작성 FAB 이 그려지지 않는다 —
 *   수신자는 로그인 사용자가 아니고 남길 기록을 쓰는 주체도 아니다.
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
    val refreshState = items.loadState.refresh
    val listError =
        if (refreshState is LoadState.Error && items.itemCount == 0) {
            refreshState.error.toListError()
        } else {
            null
        }

    when (listError) {
        ReceiverAfternoteListError.NotDeliverable -> {
            ReceiverAfternoteListErrorBody(
                title = stringResource(R.string.receiver_afternote_list_not_deliverable_message),
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
                headerDescription = stringResource(R.string.receiver_afternote_list_header_description),
                // NEXT STEP 은 «내가 남길 기록» 을 재촉하는 발신자용 카드다. 수신자 목록에는 없다.
                nextStep = null,
                modifier = modifier,
            )
        }
    }
}
