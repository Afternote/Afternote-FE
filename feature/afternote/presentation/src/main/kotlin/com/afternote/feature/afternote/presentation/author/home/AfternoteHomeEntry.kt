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
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

data class AfternoteHomeEntryActions(
    val navigateToDetail: (String) -> Unit = {},
    val navigateToGalleryDetail: (String) -> Unit = {},
    val navigateToMemorialGuidelineDetail: (String) -> Unit = {},
    val navigateToAdd: (AfternoteCategory) -> Unit = {},
)

/**
 * 애프터노트 목록 Entry.
 *
 * Paging 3 스트림을 LazyPagingItems로 수집해 Screen에 그대로 전달한다.
 * append 단계 에러는 Snackbar로만 노출하며, 사용자가 다음 페이지에 다시 진입하면
 * Paging이 자동으로 재시도한다.
 */
@Composable
fun AfternoteHomeEntry(
    viewModel: AfternoteHomeViewModel = hiltViewModel(),
    actions: AfternoteHomeEntryActions = AfternoteHomeEntryActions(),
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val items = viewModel.pagedAfternotes.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    val refreshState = items.loadState.refresh
    val isInitialLoading = refreshState is LoadState.Loading && items.itemCount == 0
    val isRefreshing = refreshState is LoadState.Loading && items.itemCount > 0

    val appendState = items.loadState.append
    val appendErrorMessage = stringResource(R.string.afternote_home_append_error)
    LaunchedEffect(appendState) {
        if (appendState is LoadState.Error) {
            snackbarHostState.showSnackbar(message = appendErrorMessage)
        }
    }

    AfternoteHomeScreen(
        items = items,
        selectedCategory = selectedCategory,
        isInitialLoading = isInitialLoading,
        isRefreshing = isRefreshing,
        snackbarHostState = snackbarHostState,
        onCategorySelected = viewModel::selectTab,
        onListItemClick = { id, type ->
            when (type) {
                AfternoteServiceType.GALLERY_AND_FILES -> actions.navigateToGalleryDetail(id)
                AfternoteServiceType.MEMORIAL -> actions.navigateToMemorialGuidelineDetail(id)
                AfternoteServiceType.SOCIAL_NETWORK -> actions.navigateToDetail(id)
            }
        },
        onRefresh = items::refresh,
        onFabClick = { actions.navigateToAdd(selectedCategory) },
    )
}
