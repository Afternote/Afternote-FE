package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.setting.domain.Passkey

internal data class PassKeyListUiState(
    val isLoading: Boolean = false,
    val passkeys: List<Passkey> = emptyList(),
    val errorMessage: UiText? = null,
)
