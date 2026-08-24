package com.afternote.feature.receiver.domain.error

/**
 * 수신자 흐름 API 가 응답을 내려주며 거절한 실패의 공통 상위 타입.
 * Retrofit·`BaseResponse` 같은 인프라 디테일은 Data 계층에서 해석한 뒤 이 계열로 통일한다.
 *
 * 하위 타입은 흐름별로 나눈다 — 한 화면이 여러 endpoint 를 호출할 때 어느 단계가 거절됐는지 타입만
 * 보고 갈라내기 위해서다. `sealed` 라 향후 사유 code 별 분기를 넣을 때 `when` 이 누락을 잡아준다.
 *
 * @property status 거절 응답의 HTTP 상태 코드. 인프라 디테일을 걷어내는 이 계열에 굳이 남기는 건
 *   "사용자 오류(4xx)"와 "장애(5xx)"를 가를 유일한 단서라서다 — 서버가 양쪽 모두에 [serverMessage] 를
 *   실어 보내고(실측 #511), 사유 `code` 체계도 둘을 분리하지 않는다. 텔레메트리 제외 판정과
 *   화면 노출 게이트(#651)가 이 값을 쓴다.
 * @property serverCode 서버 봉투의 사유 code (BE `ErrorCode` 번호 — 1902 등). 화면 노출 게이트가
 *   표시 허용 allowlist 판정에 쓴다. 서버의 `@Valid` 바디 검증 실패는 enum code 가 아니라
 *   **리터럴 400** 으로 온다(실측) — 형식 검증 문구가 allowlist 에 못 드는 건 이 때문이다.
 * @property serverMessage 백엔드가 실제로 내려준 사용자 친화 message.
 *   **null 이면 서버가 message 미제공** — 호출처는 정적 R.string 으로 폴백한다. 클라가 만든 generic
 *   문구("알 수 없는 서버 에러" 등)는 여기 들어오지 않는다.
 * @param serverMessageFallback [serverMessage] 가 null 일 때 `Throwable.message` 로 쓸 개발자용 문구 —
 *   Logcat·Crashlytics 에 단서를 남기기 위한 것이라 사용자에게 노출하지 않는다. 하위 타입이 서버
 *   `code` 를 이 문구에도 보간해 넣는다(로그 한 줄에서 사유가 보이도록).
 */
sealed class ReceiverServerRejectionException(
    val status: Int,
    val serverCode: Int,
    val serverMessage: String?,
    serverMessageFallback: String,
) : Exception(serverMessage ?: serverMessageFallback)
