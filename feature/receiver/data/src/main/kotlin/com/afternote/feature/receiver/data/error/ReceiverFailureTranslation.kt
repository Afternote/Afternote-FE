package com.afternote.feature.receiver.data.error

import com.afternote.core.network.model.ApiException
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import java.io.IOException

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
    )

/**
 * 인프라 예외를 수신자 도메인 실패로 옮긴다. **사유를 확인하지 못한 실패는 원본 그대로 돌려준다** —
 * 없는 `status`·`serverCode` 를 지어내 감싸면 그 가짜 값을 소비처가 실제 대역으로 읽는다.
 *
 * `when` 의 순서가 계약이다. [ApiException] 은 [IOException] 의 **하위 타입**이라(OkHttp
 * Interceptor 가 4xx·5xx 를 가로채 던질 때 OkHttp 가 그대로 전파해야 하므로 그렇게 선언돼 있다),
 * IO 갈래를 위에 두면 서버가 내려준 거절까지 «네트워크 없음» 으로 뭉개진다.
 *
 * 취소는 여기 오지 않는다 — 호출처의 `runCatchingCancellable` 이 `CancellationException` 을
 * 먼저 되던진다.
 */
internal fun Throwable.toReceiverFailure(): Throwable =
    when (this) {
        is ApiException -> toServerRejection()
        is IOException -> ReceiverFailure.NetworkUnavailable(this)
        else -> this
    }
