package com.afternote.feature.afternote.presentation.editor.receiver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 수신자 선택 화면 진입점 (#540).
 *
 * 선택 완료(id)는 [onReceiverConfirmed] 로 나간다 — NavHost 구현이 에디터 엔트리의
 * SavedStateHandle 에 `SELECTED_RECEIVER_ID_KEY` 로 쓰고 pop 하면, 에디터의
 * [tryApplyReceiverSelectionFromSavedState] 가 복귀 시 읽어 폼에 반영한다.
 */
@Composable
internal fun AfternoteSelectReceiverNavigation(
    onPopBackStack: () -> Unit,
    onReceiverConfirmed: (Long) -> Unit,
) {
    val viewModel: SelectReceiverViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SelectReceiverScreen(
        uiState = uiState,
        onBackClick = onPopBackStack,
        onReceiverToggle = viewModel::toggleReceiverSelection,
        onRetryClick = viewModel::refresh,
        onConfirmClick = onReceiverConfirmed,
    )
}
