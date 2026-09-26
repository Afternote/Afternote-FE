package com.afternote.feature.setting.presentation.receiver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ReceiverEditScreen(
    onBackClick: () -> Unit,
    onEditSuccess: () -> Unit,
    viewModel: ReceiverEditViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnEditSuccess by rememberUpdatedState(onEditSuccess)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ReceiverEditEvent.EditSuccess -> currentOnEditSuccess()
            }
        }
    }

    ReceiverEditContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRegister = viewModel::update,
        modifier = modifier,
    )
}
