package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.mvi.ObserveSignal
import kotlinx.coroutines.launch

/**
 * 노래 추가 Entry.
 *
 * ViewModel에서 데이터를 로드·가공하고, Screen에 전달만 합니다.
 */
@Composable
internal fun AddSongEntry(
    viewModel: AddSongViewModel,
    onBackClick: () -> Unit,
    onSongsAdded: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    ObserveSignal(
        signal = uiState.errorRes,
        consumed = AddSongIntent.ConsumeError,
        onIntent = viewModel::onIntent,
    ) { errorRes ->
        scope.launch { snackbarHostState.showSnackbar(resources.getString(errorRes), withDismissAction = true) }
    }

    AddSongScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onSongsAdded = onSongsAdded,
        modifier = modifier,
    )
}
