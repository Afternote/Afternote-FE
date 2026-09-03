package com.afternote.core.domain.error

/**
 * 사용자가 입력을 수정해 해결할 수 있는 수신자 등록·수정 요청 오류.
 *
 * **서버 원문을 접근자로 열지 않는다.** 서버 `message` 는 사용자 노출용이라는 규정이 없고(BE#92),
 * 그건 [CoreAuthFailure] 와 수신자 흐름의 `ReceiverFailure` 루트가 이미 세운 규약이다. public
 * 접근자로 열어 두면 어느 모듈이든 그 값을 그대로 화면에 실을 수 있다. 소비처는 값이 아니라
 * **타입만** 보고 로컬 리소스를 고른다(setting 의 `Throwable.toReceiverFailureMessage`).
 *
 * 원문은 진단용으로 `message` 에만 남는다. 리포팅 백엔드에는 닿지 않는다 — `core:common` 의
 * `ErrorReporter` 가 message 를 타입 이름으로 갈아치운 사본만 올린다.
 *
 * @param serverMessage 서버가 내려준 거절 사유 원문. 진단 문구로만 쓴다.
 * @param cause 이 실패를 만든 인프라 예외.
 */
class ReceiverRequestRejectedException(
    serverMessage: String,
    cause: Throwable,
) : Exception(serverMessage, cause)
