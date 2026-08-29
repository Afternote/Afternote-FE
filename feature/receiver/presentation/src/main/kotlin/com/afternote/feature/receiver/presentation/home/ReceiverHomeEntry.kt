package com.afternote.feature.receiver.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    ReceiverHomeScreen(
        modifier = modifier,
        uiState = uiState,
        onEvent = viewModel::onEvent,
        actions = actions,
    )
}
