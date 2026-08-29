package com.afternote.feature.receiver.presentation.error

import androidx.annotation.StringRes
import com.afternote.core.ui.UiText
import com.afternote.feature.receiver.domain.error.ReceiverFailure

/** 실패를 화면 문구로 옮긴다 — 노출 가능 여부 판정은 [isUserDisplayableRejection] 이 가른다. */
internal fun Throwable.toReceiverErrorUiText(
    @StringRes fallbackRes: Int,
): UiText =
    UiText.DynamicOrResource(
        value = (this as? ReceiverFailure)?.displayTextOrNull(),
        fallbackResId = fallbackRes,
    )

/**
 * 화면에 그대로 실어도 되는 서버 문구. 루트로 좁혀 `when` 을 exhaustive 하게 만든다 — 수신자 실패
 * 유형이 늘면 여기가 컴파일 에러로 잡힌다. `else` 로 뭉개 두면 새 유형이 게이트를 거치지 않은 채
 * 흘러가 노출 규약이 조용히 빈다.
 *
 * null 은 "노출할 서버 문구 없음" 이고, 폴백 리소스는 [toReceiverErrorUiText] 한 곳에서만 적용한다.
 */
private fun ReceiverFailure.displayTextOrNull(): String? =
    when (this) {
        is ReceiverFailure.ServerRejection -> {
            serverMessage?.takeIf { isUserDisplayableRejection() && it.isNotBlank() }
        }

        // 서버에 닿지 못한 실패라 노출할 서버 문구가 없다. 안내는 화면이 정적 리소스로 그린다.
        is ReceiverFailure.NetworkUnavailable -> {
            null
        }

        // 사유가 타입으로 특정된 거절이라 서버 문구를 싣지 않는다 — 표시 문구는 호출처 리소스가 갖는다.
        is ReceiverFailure.DeliveryConditionNotMet -> {
            null
        }
    }

/**
 * status 만 보고 code 를 안 보면 4xx 검증류 개발자 문구까지 화면에 실린다 — `@Valid` 실패가
 * "인증번호는 UUID 형식이어야 합니다." 를 리터럴 code=400 으로 실어 보낸 것(#600 실측)이 그 반례다.
 * 문구 유무는 여기서 보지 않는다 — 호출부 체인이 거른다.
 */
private fun ReceiverFailure.ServerRejection.isUserDisplayableRejection(): Boolean =
    status in DISPLAYABLE_CLIENT_ERROR_RANGE && serverCode in USER_DISPLAYABLE_SERVER_CODES

/** 등재 code 라도 5xx 봉투면 장애다 — code 게이트와 독립으로 대역을 한 번 더 본다. */
private val DISPLAYABLE_CLIENT_ERROR_RANGE = 400..499

/**
 * 서버는 표시 가능 여부를 알려주지 않는다 — BE `ErrorCode` 의 실제 문구가 사용자 안내인 것만 골라
 * 등재한 목록이다. 신규 code 는 문구를 확인한 뒤에만 더한다. 미등재 기본값은 폴백.
 */
private val USER_DISPLAYABLE_SERVER_CODES = setOf(1900, 1901, 1902, 1903, 2008)
