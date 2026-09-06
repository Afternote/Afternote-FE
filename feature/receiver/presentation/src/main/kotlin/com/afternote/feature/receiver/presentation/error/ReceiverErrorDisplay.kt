package com.afternote.feature.receiver.presentation.error

import androidx.annotation.StringRes
import com.afternote.core.ui.UiText
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import com.afternote.feature.receiver.presentation.R

/** 실패를 화면 문구로 옮긴다. 서버 응답의 노출 가능 여부는 Data 계층이 [ReceiverRejectionReason] 으로 번역했다. */
internal fun Throwable.toReceiverErrorUiText(
    @StringRes fallbackRes: Int,
): UiText = (this as? ReceiverFailure)?.displayUiTextOrNull() ?: UiText.Resource(fallbackRes)

/** 새 수신자 실패 유형이 생기면 노출 정책도 함께 정하도록 도메인 루트의 `when` 을 exhaustive 하게 유지한다. */
private fun ReceiverFailure.displayUiTextOrNull(): UiText? =
    when (this) {
        is ReceiverFailure.UserRejection -> {
            reason?.toUiText()
        }

        is ReceiverFailure.UnexpectedServerFailure -> {
            null
        }

        is ReceiverFailure.NetworkUnavailable -> {
            null
        }

        is ReceiverFailure.DeliveryConditionNotMet -> {
            null
        }

        // 「아직 만들지 않았다」에 전용 문구를 두지 않는다 — 호출처 폴백(내려받기 실패 안내)이 맞다.
        is ReceiverFailure.ExportNotSupported -> {
            null
        }
    }

/**
 * 서버 code 에 대응하던 고정 문구를 로컬 리소스로 옮긴다. 신규 사유는 Data 계층의 code 번역과
 * 여기의 리소스 매핑을 함께 추가해야 하므로, enum `when` 을 exhaustive 하게 유지한다.
 */
private fun ReceiverRejectionReason.toUiText(): UiText =
    when (this) {
        ReceiverRejectionReason.INVALID_AUTH_CODE -> {
            UiText.Resource(R.string.receiver_error_invalid_auth_code)
        }

        ReceiverRejectionReason.RECEIVER_EMAIL_NOT_FOUND -> {
            UiText.Resource(R.string.receiver_error_email_not_found)
        }

        ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND -> {
            UiText.Resource(R.string.receiver_error_email_auth_code_not_found)
        }

        ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_MISMATCH -> {
            UiText.Resource(R.string.receiver_error_email_auth_code_mismatch)
        }

        ReceiverRejectionReason.VERIFICATION_ALREADY_SUBMITTED -> {
            UiText.Resource(R.string.receiver_error_verification_already_submitted)
        }
    }
