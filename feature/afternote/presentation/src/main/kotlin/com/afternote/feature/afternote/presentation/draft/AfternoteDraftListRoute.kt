package com.afternote.feature.afternote.presentation.draft

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import kotlinx.coroutines.flow.map

/**
 * 임시저장 목록 Entry — append 실패는 홈과 같이 Snackbar 로만 알린다.
 */
@Composable
internal fun AfternoteDraftListEntry(
    onBackClick: () -> Unit,
    onResumeDraft: (id: Long, type: AfternoteType) -> Unit,
    viewModel: AfternoteDraftListViewModel = hiltViewModel(),
) {
    val pages = remember(viewModel) { viewModel.uiState.map { it.items } }
    val items = pages.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    val appendState = items.loadState.append
    val appendErrorMessage = stringResource(R.string.afternote_home_append_error)
    LaunchedEffect(appendState) {
        if (appendState is LoadState.Error) {
            snackbarHostState.showSnackbar(message = appendErrorMessage)
        }
    }

    AfternoteDraftListScreen(
        items = items,
        onBackClick = onBackClick,
        onDraftClick = onResumeDraft,
        snackbarHostState = snackbarHostState,
    )
}
