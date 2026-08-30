package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R

/**
 * 애프터노트 목록 Entry.
 *
 * Paging 3 스트림을 LazyPagingItems로 수집해 Screen에 그대로 전달한다.
 * append 단계 에러는 Snackbar로만 노출하며, 사용자가 다음 페이지에 다시 진입하면
 * Paging이 자동으로 재시도한다.
 */
@Composable
fun AfternoteHomeEntry(
    navigateToDetail: (Long) -> Unit,
    navigateToAdd: (AfternoteType) -> Unit,
    onSettingClick: () -> Unit,
    viewModel: AfternoteHomeViewModel = hiltViewModel(),
) {
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val items = viewModel.pagedAfternotes.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    val appendState = items.loadState.append
    val appendErrorMessage = stringResource(R.string.afternote_home_append_error)
    LaunchedEffect(appendState) {
        if (appendState is LoadState.Error) {
            snackbarHostState.showSnackbar(message = appendErrorMessage)
        }
    }

    AfternoteHomeScreen(
        items = items,
        selectedType = selectedType,
        snackbarHostState = snackbarHostState,
        onTypeSelected = viewModel::selectTab,
        onListItemClick = { id, type ->
            when (type) {
                AfternoteType.SOCIAL_NETWORK,
                AfternoteType.BUSINESS,
                AfternoteType.GALLERY_AND_FILES,
                AfternoteType.MEMORIAL,
                -> navigateToDetail(id)

                // ESTATE 는 placeholder 카테고리. 서버 미지원이라 리스트에 노출되지 않으므로 도달 시 무시.
                AfternoteType.ESTATE -> Unit
            }
        },
        // NEXT STEP 카드는 시안·컴포넌트가 다 있으나 «다음에 무엇을 하라» 를 만드는 원천이
        // 서버에도 ViewModel 에도 없다. 여기서 null 을 넘기면 카드가 뜨지 않는다 — 종전에는
        // InfiniteListBody 의 `= {}` 디폴트가 이 공백을 «탭해도 반응 없는 카드» 로 덮고 있었다 (#777).
        nextStep = null,
        // 0건이어도 헤더를 그린다 — 시안 `애프터노트_목록X`(4327:66762)에 제목·설명·NEXT STEP 이 모두 있다.
        // 종전에는 이 경로가 EmptyListBody 만 그려 첫 진입 사용자에게 화면 제목이 보이지 않았다 (#1175).
        showsHeaderOnEmptyList = true,
        onFabClick = { navigateToAdd(selectedType ?: AfternoteType.SOCIAL_NETWORK) },
        onSettingClick = onSettingClick,
        headerDescription = stringResource(R.string.afternote_home_header_description),
    )
}
