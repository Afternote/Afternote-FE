package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.compose.runtime.Immutable

@Immutable
data class MasterKeyUiState(
    val isSubmitting: Boolean = false,
    /** 표시할 에러 — null 이면 에러 없음. 서버 message 는 [ErrorPayload.Text], 클라 fallback 은 [ErrorPayload.Res]. */
    val error: ErrorPayload? = null,
    val isVerified: Boolean = false,
)
