package com.afternote.feature.mindrecord.domain.error

/**
 * 전달 조건이 아직 충족되지 않아 수신자가 기록을 열람할 수 없는 상태 (#614).
 *
 * 수신자가 할 수 있는 일이 없다 — 발신자가 전달 조건을 설정해야 풀린다. 화면은 이 타입만
 * 보고 문구를 고르고, 어떤 서버 코드에서 왔는지는 data 계층의 매핑만 안다.
 */
class DeliveryNotReadyException(
    cause: Throwable? = null,
) : Exception(cause)
