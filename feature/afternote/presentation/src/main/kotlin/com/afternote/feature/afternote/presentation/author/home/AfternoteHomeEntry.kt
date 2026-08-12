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
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

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
    navigateToDetail: (String) -> Unit = {},
    navigateToGalleryDetail: (String) -> Unit = {},
    navigateToMemorialDetail: (String) -> Unit = {},
    navigateToAdd: (AfternoteCategory) -> Unit = {},
    onSettingClick: () -> Unit = {},
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
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
        selectedCategory = selectedCategory,
        snackbarHostState = snackbarHostState,
        onCategorySelected = viewModel::selectTab,
        onListItemClick = { id, type ->
            when (type) {
                AfternoteType.GALLERY_AND_FILES -> navigateToGalleryDetail(id)

                AfternoteType.MEMORIAL -> navigateToMemorialDetail(id)

                // BUSINESS 상세는 소셜 상세 화면을 재사용한다 (구성 동일: 계정 정보·처리 방법·남긴 말씀 — 이슈 #467).
                AfternoteType.SOCIAL_NETWORK, AfternoteType.BUSINESS -> navigateToDetail(id)

                // ESTATE 는 placeholder 카테고리. 서버 미지원이라 리스트에 노출되지 않으므로 도달 시 무시.
                AfternoteType.ESTATE -> Unit
            }
        },
        onFabClick = { navigateToAdd(selectedCategory) },
        onSettingClick = onSettingClick,
    )
}
