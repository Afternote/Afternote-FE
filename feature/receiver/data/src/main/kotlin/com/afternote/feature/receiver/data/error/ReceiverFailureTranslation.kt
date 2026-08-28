package com.afternote.feature.receiver.data.error

import com.afternote.core.network.model.ApiException
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import java.io.IOException

/** BE `ErrorCode.DELIVERY_CONDITION_NOT_MET(HttpStatus.FORBIDDEN, 2009, "아직 전달 조건이 충족되지 않았습니다.")`. */
private const val DELIVERY_CONDITION_NOT_MET = 2009

/**
 * 인프라 예외를 수신자 도메인 실패로 옮긴다. **사유를 확인하지 못한 실패는 원본 그대로 돌려준다** —
 * 없는 `status`·`serverCode` 를 지어내 감싸면 그 가짜 값을 소비처가 실제 대역으로 읽는다.
 *
 * 서버가 응답하며 거절한 [ApiException]과 서버에 닿지 못한 [IOException]을 별도 도메인 타입으로
 * 옮긴다. 둘은 Retrofit CallAdapter 경계부터 서로 다른 타입 계열이다.
 *
 * 취소는 여기 오지 않는다 — 호출처의 `runCatchingCancellable` 이 `CancellationException` 을
 * 먼저 되던진다.
 */
internal fun Throwable.toReceiverFailure(): Throwable =
    when (this) {
        is ApiException -> toReceiverRejection()
        is IOException -> ReceiverFailure.NetworkUnavailable(this)
        else -> this
    }

/**
 * 서버 사유 code 를 도메인 어휘로 가른다. **BE `ErrorCode` 번호를 아는 것은 이 계층까지다** —
 * 소비처가 번호를 다시 보게 하면 서버의 code 체계가 presentation 까지 샌다.
 *
 * 지금 갈라 세운 사유는 하나뿐이다. 화면 처리가 실제로 달라지는 것만 타입으로 올리고, 나머지는
 * [ReceiverFailure.ServerRejection] 이 code 를 실어 나른다 — 그 남은 소비처(표시 허용 allowlist)를
 * 이 계층으로 내리는 것은 #1053 범위다.
 */
private fun ApiException.toReceiverRejection(): ReceiverFailure =
    when (code) {
        DELIVERY_CONDITION_NOT_MET -> ReceiverFailure.DeliveryConditionNotMet(this)
        else -> toServerRejection()
    }

/**
 * 서버가 응답을 내려주며 거절한 실패를 도메인 어휘로 옮긴다.
 *
 * 같은 세 필드를 옮기는 변환이 호출처마다 인라인으로 흩어져 있으면, 필드가 늘 때 일부만 고쳐진
 * 채로 남는다 — 실제로 `serverCode` 가 뒤늦게 추가됐을 때 목록 경로는 번역 자체가 없어 인프라
 * 타입이 도메인 밖으로 그대로 샜다(#611). 변환을 한 자리에 둬 그 갈라짐을 없앤다.
 */
internal fun ApiException.toServerRejection(): ReceiverFailure.ServerRejection =
    ReceiverFailure.ServerRejection(
        status = status,
        serverCode = code,
        serverMessage = serverMessage,
        cause = this,
    )
