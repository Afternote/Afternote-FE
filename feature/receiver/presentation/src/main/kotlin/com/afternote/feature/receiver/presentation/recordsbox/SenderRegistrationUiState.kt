package com.afternote.feature.receiver.presentation.recordsbox

import androidx.compose.runtime.Immutable
import com.afternote.core.ui.mvi.UiState

@Immutable
internal data class SenderRegistrationUiState(
    val isRegistered: Boolean = false,
) : UiState
