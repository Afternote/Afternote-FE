package com.afternote.feature.home.presentation.receiver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 수신자 홈 Entry — Hilt VM에서 단일 UI State를 collect하고 [ReceiverHomeScreen]에 흘려준다.
 */
@Composable
fun ReceiverHomeEntry(
    actions: ReceiverHomeActions,
    modifier: Modifier = Modifier,
    viewModel: ReceiverHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 상세 등 다른 화면에서 홈으로 복귀하면 다시 조회한다 — 백스택에 살아 있는 동안 옛 값이
    // 남지 않게 한다 (#701). 로딩을 방출하지 않는 refreshOnReturn() 을 쓴다. 첫 진입의
    // ON_RESUME 스킵(진입은 init 로드가 담당)과 실행 중 로드와의 중복 차단은 VM 이 판단한다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    ReceiverHomeScreen(
        modifier = modifier,
        uiState = uiState,
        onEvent = viewModel::onEvent,
        actions = actions,
    )
}
