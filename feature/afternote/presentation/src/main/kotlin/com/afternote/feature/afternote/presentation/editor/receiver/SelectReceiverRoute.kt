package com.afternote.feature.afternote.presentation.editor.receiver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 수신자 선택 화면 진입점 (#540).
 *
 * 확정한 수신자 id 전체는 [onReceiversConfirmed] 로 나간다 — NavHost 구현이 에디터 엔트리의
 * SavedStateHandle 에 `SELECTED_RECEIVER_IDS_KEY` 로 쓰고 pop 하면, 에디터의
 * [tryApplyReceiverSelectionFromSavedState] 가 복귀 시 읽어 폼에 반영한다.
 *
 * [preselectedReceiverIds] 는 에디터 폼에 이미 들어 있는 수신자다. 화면을 그 선택 상태로 열어야
 * 사용자가 «이미 지정한 사람» 을 다시 고르거나 풀 수 있다 (#1426).
 */
@Composable
internal fun AfternoteSelectReceiverNavigation(
    preselectedReceiverIds: List<Long>,
    onPopBackStack: () -> Unit,
    onReceiversConfirmed: (List<Long>) -> Unit,
) {
    val viewModel: SelectReceiverViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 진입 시점의 폼에 담겨 있던 수신자만 체크한다. 회전·재진입으로 컴포지션이 새로 만들어지면 이 효과도
    // 다시 돌지만, ViewModel 이 최초 1회만 받아 사용자가 푼 체크가 되살아나지 않는다.
    LaunchedEffect(Unit) { viewModel.applyPreselection(preselectedReceiverIds) }

    SelectReceiverScreen(
        uiState = uiState,
        onBackClick = onPopBackStack,
        onReceiverToggle = viewModel::toggleReceiverSelection,
        onRetryClick = viewModel::refresh,
        onConfirmClick = onReceiversConfirmed,
    )
}
