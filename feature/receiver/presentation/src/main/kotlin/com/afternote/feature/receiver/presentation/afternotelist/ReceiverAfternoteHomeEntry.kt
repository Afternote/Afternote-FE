package com.afternote.feature.receiver.presentation.afternotelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.feature.afternote.presentation.author.home.AfternoteHomeScreen
import com.afternote.feature.receiver.presentation.R

/**
 * 수신자 애프터노트 목록 Entry.
 *
 * 작성자 측과 동일한 Paging 3 스트림 + [AfternoteHomeScreen]을 그대로 재사용한다. 다만 화면을 공유해도
 * 관점은 다르므로 발신자용 조각은 넘기지 않는다 (#620):
 * - 헤더 문구는 수신자 관점으로 덮는다 (기본값이 없어 안 넘기면 컴파일이 막는다).
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

    AfternoteHomeScreen(
        items = items,
        selectedCategory = selectedTab,
        onCategorySelected = viewModel::selectTab,
        onListItemClick = { id, _ -> navigateToDetail(id) },
        headerDescription = stringResource(R.string.receiver_afternote_list_header_description),
        modifier = modifier,
    )
}
