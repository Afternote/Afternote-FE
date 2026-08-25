package com.afternote.feature.receiver.data.error

import com.afternote.core.network.model.ApiException
import com.afternote.feature.receiver.domain.error.ReceiverFailure

/**
 * 인프라 예외([ApiException])를 수신자 도메인 실패로 옮긴다.
 *
 * 같은 세 필드를 옮기는 변환이 호출처마다 인라인으로 흩어져 있으면, 필드가 늘 때 일부만 고쳐진
 * 채로 남는다 — 실제로 `serverCode` 가 뒤늦게 추가됐을 때 목록 경로는 번역 자체가 없어 인프라
 * 타입이 도메인 밖으로 그대로 샜다(#611). 변환을 한 자리에 둬 그 갈라짐을 없앤다.
 *
 * 취소는 여기 오지 않는다 — [ApiException] 만 받으므로 `CancellationException` 은 호출처의
 * `runCatchingCancellable` 이 그대로 되던진다.
 */
internal fun ApiException.toServerRejection(): ReceiverFailure.ServerRejection =
    ReceiverFailure.ServerRejection(
        status = status,
        serverCode = code,
        serverMessage = serverMessage,
    )
