package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.runtime.Immutable
import com.afternote.core.ui.UiText
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopup

@Immutable
data class IdentityVerificationUiState(
    val email: String = "",
    val code: String = "",
    val isEmailFormatValid: Boolean = false,
    val isVerificationSent: Boolean = false,
    val isSendingCode: Boolean = false,
    val isVerifying: Boolean = false,
    /**
     * **표시 대기 중인** 에러. `null` 은 «실패가 없었다» 가 아니라 «지금 띄울 것이 없다» 다 — 화면이
     * 한 번 표시한 뒤 `consumeError()` 로 되돌리므로, 실패한 적 없는 상태와 이미 보여 준 상태가 같은
     * 값이 된다. 마지막 시도의 성패를 알아야 하면 [isVerifying]·[isVerified] 를 본다.
     *
     * 표시 가능한 서버 message 와 클라이언트 fallback 을 [UiText] 하나로 운반한다. [isVerified] 와 같은 소비형 필드다.
     */
    val errorMessage: UiText? = null,
    /**
     * 표시 중인 공통 오류 팝업(#446). [errorMessage] 와 배타적이다 — 서버 작업 실패는 팝업이 정본이고,
     * 스낵바 자리에는 서버가 준 거절 사유(코드 불일치·미등록 이메일 등)만 남는다.
     *
     * [errorMessage] 와 달리 표시 즉시 소비하지 않는다 — 모달이라 사용자가 재시도·닫기 중 하나를 고를
     * 때까지 떠 있다.
     */
    val errorPopup: ReceiverErrorPopup? = null,
    val isVerified: Boolean = false,
) {
    val canSubmit: Boolean
        get() =
            isVerificationSent &&
                code.trim().isNotEmpty() &&
                !isVerifying
}
