package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.runtime.Immutable
import com.afternote.feature.receiver.presentation.error.ErrorPayload

@Immutable
data class IdentityVerificationUiState(
    val email: String = "",
    val code: String = "",
    val isEmailFormatValid: Boolean = false,
    val isVerificationSent: Boolean = false,
    val isSendingCode: Boolean = false,
    val isVerifying: Boolean = false,
    /** 표시할 에러 — null 이면 에러 없음. 서버 message 는 [ErrorPayload.Text], 클라 fallback 은 [ErrorPayload.Res]. */
    val error: ErrorPayload? = null,
    val isVerified: Boolean = false,
) {
    val canSubmit: Boolean
        get() =
            isVerificationSent &&
                code.trim().isNotEmpty() &&
                !isVerifying
}
