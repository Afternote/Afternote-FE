package com.afternote.feature.setting.presentation.receiver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.mvi.ObserveSignal

@Composable
internal fun ReceiverEditScreen(
    onBackClick: () -> Unit,
    onEditSuccess: () -> Unit,
    viewModel: ReceiverEditViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveSignal(
        signal = uiState.pendingEvent,
        consumed = ReceiverEditIntent.ConsumeSuccess,
        onIntent = viewModel::onIntent,
        onSignal = { onEditSuccess() },
    )

    ReceiverEditContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRegister = { name, relation, phone, email, message ->
            viewModel.onIntent(ReceiverEditIntent.Update(name, relation, phone, email, message))
        },
        modifier = modifier,
    )
}
