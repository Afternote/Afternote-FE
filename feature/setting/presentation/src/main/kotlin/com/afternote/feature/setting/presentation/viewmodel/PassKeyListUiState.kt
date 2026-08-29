package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.model.user.Passkey

data class PassKeyListUiState(
    val isLoading: Boolean = false,
    val passkeys: List<Passkey> = emptyList(),
    val errorMessage: String? = null,
)
