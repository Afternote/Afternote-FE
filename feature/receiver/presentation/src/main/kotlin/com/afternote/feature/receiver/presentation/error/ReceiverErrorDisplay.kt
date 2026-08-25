package com.afternote.feature.receiver.presentation.error

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.feature.receiver.domain.error.ReceiverFailure

/**
 * 화면이 표시할 에러 문구를 VM → UI 로 실어 나른다. 리소스와 서버 동적 문구를 각각 nullable 필드로
 * 두면 둘 다 set 되는 상태가 가능해지므로, sealed 로 "하나만" 을 타입에 강제한다.
 *
 * 둘을 String 하나로 합칠 수는 없다 — 리소스 ID → String 변환에 Context 가 필요해 VM 에서 못 풀고
 * (UI 의 stringResource 가 마지막에 한 번), 서버 동적 문구는 리소스가 될 수 없다.
 */
sealed interface ErrorPayload {
    /** 클라이언트가 미리 정의한 generic 문구 (i18n 가능). 서버 message 미제공·5xx 장애 시 fallback. */
    data class Res(
        @param:StringRes val id: Int,
    ) : ErrorPayload

    /** 백엔드가 런타임에 내려준 사용자 친화 message (예: 409 "이미 대기 중인 인증 요청이 존재합니다."). */
    data class Text(
        val message: String,
    ) : ErrorPayload
}

/** 실패를 화면 문구로 옮긴다 — 노출 가능 여부 판정은 [isUserDisplayableRejection] 이 가른다. */
internal fun Throwable.toErrorPayload(
    @StringRes fallbackRes: Int,
): ErrorPayload =
    (this as? ReceiverFailure)
        ?.displayTextOrNull()
        ?.let { ErrorPayload.Text(it) }
        ?: ErrorPayload.Res(fallbackRes)

/**
 * 화면에 그대로 실어도 되는 서버 문구. 루트로 좁혀 `when` 을 exhaustive 하게 만든다 — 수신자 실패
 * 유형이 늘면 여기가 컴파일 에러로 잡힌다. `else` 로 뭉개 두면 새 유형이 게이트를 거치지 않은 채
 * 흘러가 노출 규약이 조용히 빈다.
 *
 * null 은 "노출할 서버 문구 없음" 이고, 폴백 리소스는 [toErrorPayload] 한 곳에서만 적용한다.
 */
private fun ReceiverFailure.displayTextOrNull(): String? =
    when (this) {
        is ReceiverFailure.ServerRejection -> {
            serverMessage?.takeIf { isUserDisplayableRejection() && it.isNotBlank() }
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
private val USER_DISPLAYABLE_SERVER_CODES = setOf(1900, 1901, 1902, 1903, 2008, DELIVERY_CONDITION_NOT_MET)

/**
 * 전달 조건 미충족 — BE `ErrorCode.DELIVERY_CONDITION_NOT_MET` (403 / 2009,
 * "아직 전달 조건이 충족되지 않았습니다."). 발신자가 수신자별 전달 조건을 아직 세우지 않았거나
 * 조건이 충족되지 않은 상태다.
 *
 * 다른 등재 code 와 달리 이름을 붙인 건 [USER_DISPLAYABLE_SERVER_CODES] 말고도 소비처가 있어서다 —
 * 목록 화면은 이 실패를 «재시도로 풀리는 실패» 와 갈라 그린다([isDeliveryConditionNotMet]).
 */
internal const val DELIVERY_CONDITION_NOT_MET = 2009

/**
 * 전달 조건이 아직 충족되지 않아 거절된 실패인가.
 *
 * 재시도로 풀리지 않는 유일한 목록 실패라 화면이 이 하나만 따로 가른다 — 나머지(네트워크·5xx·기타
 * 4xx)는 재시도가 유효하므로 종전 경로에 남는다. 사유가 더 늘면 그때 sealed 로 올린다.
 */
internal fun Throwable.isDeliveryConditionNotMet(): Boolean =
    this is ReceiverFailure.ServerRejection && serverCode == DELIVERY_CONDITION_NOT_MET

/**
 * 화면에 그릴 최종 문자열. 리소스 해석에 Context 가 필요해 VM 에서는 못 풀고 여기서 한 번에 푼다 —
 * `when` 을 소비처마다 펼치면 새 [ErrorPayload] 갈래가 생겼을 때 일부만 고쳐진 채 남는다.
 */
@Composable
internal fun ErrorPayload.asDisplayText(): String =
    when (this) {
        is ErrorPayload.Res -> stringResource(id)
        is ErrorPayload.Text -> message
    }
