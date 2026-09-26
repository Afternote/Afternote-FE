package com.afternote.feature.receiver.presentation.recordsbox

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.mvi.ObserveSignal

/**
 * 발신자 등록 화면(15·16) — FAB 에서 진입하는 이름 입력 화면 (이슈 #215).
 *
 * 이름이 공백이 아닐 때만 "발신자 등록하기" 버튼 활성화 (15 비활성 → 16 활성).
 * 등록 완료 시 받은 기록함으로 pop, 카드가 추가된 채로 노출된다.
 */
@Composable
internal fun SenderRegistrationScreen(
    onBackClick: () -> Unit,
    onRegistered: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SenderRegistrationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nameState = rememberTextFieldState()

    ObserveSignal(
        signal = Unit.takeIf { uiState.isRegistered },
        consumed = SenderRegistrationIntent.ConsumeRegistered,
        onIntent = viewModel::onIntent,
        onSignal = { onRegistered() },
    )

    SenderRegistrationScreenContent(
        nameState = nameState,
        onBackClick = onBackClick,
        onSubmitClick = { viewModel.onIntent(SenderRegistrationIntent.Submit(nameState.text.toString())) },
        modifier = modifier,
    )
}
