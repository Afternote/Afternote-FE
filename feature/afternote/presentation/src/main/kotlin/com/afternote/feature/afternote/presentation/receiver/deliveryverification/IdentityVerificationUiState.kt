package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.compose.runtime.Immutable

@Immutable
data class IdentityVerificationUiState(
    val email: String = "",
    val code: String = "",
    val isEmailFormatValid: Boolean = false,
    val isVerificationSent: Boolean = false,
    val isSendingCode: Boolean = false,
    val isVerifying: Boolean = false,
    val errorMessageRes: Int? = null,
) {
    val canSubmit: Boolean
        get() =
            isVerificationSent &&
                code.trim().isNotEmpty() &&
                !isVerifying
}

sealed interface IdentityVerificationEvent {
    data object Verified : IdentityVerificationEvent
}
