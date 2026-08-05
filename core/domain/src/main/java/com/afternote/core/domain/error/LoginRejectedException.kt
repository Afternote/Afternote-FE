package com.afternote.core.domain.error

/**
 * 서버가 사용자에게 보여 줄 사유를 명시한 로그인 거절.
 *
 * 표시 가능 여부 판정(사유 코드 allowlist)은 data 계층에 있고 [displayMessage] 는 그 판정을 통과한
 * 값만 담는다 — presentation 은 이 타입 여부만으로 원문 노출을 가를 수 있다.
 *
 * `Throwable.message` 대신 별도 프로퍼티인 이유 — `e.message ?: 폴백` 소비처가 판정을 건너뛰고
 * 문구를 꺼내 쓰는 길을 막기 위해서다([NetworkUnavailableException] 이 message 를 비우는 것과 같은 방향).
 */
class LoginRejectedException(
    val displayMessage: String,
    cause: Throwable,
) : Exception(null, cause)
