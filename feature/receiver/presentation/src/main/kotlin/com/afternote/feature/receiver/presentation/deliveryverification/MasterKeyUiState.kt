package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.runtime.Immutable
import com.afternote.feature.receiver.presentation.error.ErrorPayload

@Immutable
data class MasterKeyUiState(
    val isSubmitting: Boolean = false,
    /**
     * **표시 대기 중인** 에러. `null` 은 «실패가 없었다» 가 아니라 «지금 띄울 것이 없다» 다 — 화면이
     * 한 번 표시한 뒤 `consumeError()` 로 되돌리므로, 실패한 적 없는 상태와 이미 보여 준 상태가 같은
     * 값이 된다. 마지막 시도의 성패를 알아야 하면 [isSubmitting]·[isVerified] 를 본다.
     *
     * 서버 message 는 [ErrorPayload.Text], 클라 fallback 은 [ErrorPayload.Res]. [isVerified] 와 같은 소비형 필드다.
     */
    val error: ErrorPayload? = null,
    val isVerified: Boolean = false,
)
