package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.compose.runtime.Immutable

@Immutable
data class MasterKeyUiState(
    val isSubmitting: Boolean = false,
    val errorMessageRes: Int? = null,
    val isVerified: Boolean = false,
)
