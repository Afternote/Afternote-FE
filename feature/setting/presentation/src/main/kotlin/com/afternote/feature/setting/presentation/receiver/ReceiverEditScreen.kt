package com.afternote.feature.setting.presentation.receiver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun ReceiverEditScreen(
    onBackClick: () -> Unit,
    onEditSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiverEditViewModel = hiltViewModel(),
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
