package com.afternote.feature.afternote.presentation.author.editor.receiver.select

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 수신자 선택 화면 진입점 (#540).
 *
 * 선택 완료(id)는 [onReceiverConfirmed] 로 나간다 — NavHost 구현이 에디터 엔트리의
 * SavedStateHandle 에 `SELECTED_RECEIVER_ID_KEY` 로 쓰고 pop 하면, 에디터의
 * [tryApplyReceiverSelectionFromSavedState] 가 복귀 시 읽어 폼에 반영한다.
 *
 * 설정의 수신자 등록 화면은 이 화면 **위로** push 된다(#1427). 에디터는 백스택에 그대로
 * 남으므로 작성 중 내용은 등록·취소 어느 쪽으로 끝나도 유지된다. 등록 화면이 pop 되어
 * 돌아온 ON_RESUME 에서 목록을 재조회하고 방금 등록한 수신자를 선택 상태로 만든다.
 */
@Composable
internal fun AfternoteSelectReceiverNavigation(
    onPopBackStack: () -> Unit,
    onReceiverConfirmed: (Long) -> Unit,
) {
    val viewModel: SelectReceiverViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 첫 진입에서도 뜨지만 등록 왕복을 거치지 않았으면 ViewModel 이 그냥 무시한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAfterReceiverRegister()
    }

    SelectReceiverScreen(
        uiState = uiState,
        onBackClick = onPopBackStack,
        onReceiverToggle = viewModel::toggleReceiverSelection,
        onRetryClick = viewModel::refresh,
        onConfirmClick = onReceiverConfirmed,
    )
}
