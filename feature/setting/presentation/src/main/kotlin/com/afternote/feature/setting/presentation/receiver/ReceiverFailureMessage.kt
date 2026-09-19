package com.afternote.feature.setting.presentation.receiver

import androidx.annotation.StringRes
import com.afternote.core.domain.error.ReceiverRequestRejectedException
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R

/**
 * [ReceiverRequestRejectedException] 은 **타입만** 보고 문구를 고른다. 서버 원문은
 * 사용자 화면에 노출하지 않으므로(BE#92 — 서버 `message` 는 사용자 노출용이라는 규정이 없다), 표시
 * 문구는 여기 로컬 리소스가 갖는다 — feature/receiver 의 ReceiverFailureTranslation.kt 이 확정한
 * 선례(#1339)와 같은 규칙이다.
 *
 * 예외의 `message`도 고정 진단 문구이며 사용자 표시 문구로 사용하지 않는다.
 */
internal fun Throwable.toReceiverFailureMessage(
    @StringRes fallbackResId: Int,
): UiText =
    if (this is ReceiverRequestRejectedException) {
        UiText.Resource(R.string.receiver_request_rejected)
    } else {
        UiText.Resource(fallbackResId)
    }
