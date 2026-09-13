package com.afternote.feature.setting.presentation.viewmodel

import androidx.annotation.StringRes
import com.afternote.core.domain.error.ReceiverRequestRejectedException
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R

/**
 * [ReceiverRequestRejectedException] 은 **타입만** 보고 문구를 고른다. 그 예외가 나르는 서버 원문은
 * 사용자 화면에 노출하지 않으므로(BE#92 — 서버 `message` 는 사용자 노출용이라는 규정이 없다), 표시
 * 문구는 여기 로컬 리소스가 갖는다 — feature/receiver 의 ReceiverFailureTranslation.kt 이 확정한
 * 선례(#1339)와 같은 규칙이다.
 *
 * 그래서 그 예외는 원문을 읽을 접근자를 두지 않는다. 값은 진단용 `message` 에만 남는다.
 */
internal fun Throwable.toReceiverFailureMessage(
    @StringRes fallbackResId: Int,
): UiText =
    if (this is ReceiverRequestRejectedException) {
        UiText.Resource(R.string.receiver_request_rejected)
    } else {
        UiText.Resource(fallbackResId)
    }
