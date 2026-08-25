package com.afternote.feature.receiver.domain.error

/**
 * 수신자 흐름(본인 확인 이메일 인증·마스터 키 검증·전달 자격 심사)의 실패 중 **사유가 확인된 것**의
 * 도메인 루트. Retrofit·`BaseResponse` 같은 인프라 디테일은 Data 계층이 해석한 뒤 이 계열로 통일한다.
 *
 * 소비처는 이 루트로 좁힌 뒤 `when` 으로 가른다 — 실패 유형이 늘면 컴파일러가 소비처를 잡아준다.
 * 사유를 확인하지 못한 실패는 번역하지 않고 원본 그대로 흘려보내므로, 소비처의 «루트가 아닌
 * Throwable» 분기는 계속 필요하다 ([com.afternote.core.domain.error.CoreAuthFailure] 와 같은 규약).
 *
 * `message` 는 **리포팅 콘솔용 정적 진단 문구**다 — 화면에 싣지 않는다(표시 문구는 사유별 매핑이
 * 갖는다). 파라미터를 non-null 로 좁혀 하위 타입이 빠뜨릴 수 없게 했다: 비워 두면 `Exception` 이
 * `cause.toString()`(`java.net.UnknownHostException: Unable to resolve host ...` 같은 영문 기술
 * 원문)을 message 로 앉힌다. `cause` 도 기본값을 두지 않아, 원인 예외가 없는 하위 타입이 그 사실을
 * `null` 로 명시하게 한다. `cause` 도 non-null 이다 — 이 계열은 전부 인프라 예외를 옮겨 만들어지므로
 * 원인이 없는 실패가 존재하지 않는다. 버리면 원 호출 지점의 stack trace 가 끊겨 Crashlytics 에서
 * «어디서 난 실패인지» 를 잃는다. (`CoreAuthFailure` 루트가 nullable 인 건 사용자 취소처럼 예외에서
 * 유래하지 않는 갈래를 갖기 때문이고, 이 계열엔 그런 갈래가 없다.)
 */
sealed class ReceiverFailure(
    message: String,
    cause: Throwable,
) : Exception(message, cause) {
    /**
     * 서버가 응답을 내려주며 거절한 실패.
     *
     * 흐름별로 타입을 나누지 않는다 — 어느 endpoint 가 거절했는지는 stack trace 에 남고, 타입으로
     * 갈라 보던 프로덕션 코드가 없었다(#934 실측). 구분이 실제로 필요해지면 그때 필드를 더한다.
     *
     * @property status 거절 응답의 HTTP 상태 코드. 인프라 디테일을 걷어내는 이 계열에 굳이 남기는 건
     *   "사용자 오류(4xx)"와 "장애(5xx)"를 가를 유일한 단서라서다 — 서버가 양쪽 모두에 [serverMessage] 를
     *   실어 보내고(실측 #511), 사유 `code` 체계도 둘을 분리하지 않는다. 텔레메트리 제외 판정과
     *   화면 노출 게이트(#651)가 이 값을 쓴다.
     * @property serverCode 서버 봉투의 사유 code (BE `ErrorCode` 번호 — 1902 등). 화면 노출 게이트가
     *   표시 허용 allowlist 판정에 쓴다. 서버의 `@Valid` 바디 검증 실패는 enum code 가 아니라
     *   **리터럴 400** 으로 온다(실측) — 형식 검증 문구가 allowlist 에 못 드는 건 이 때문이다.
     * @param cause 이 거절을 만든 인프라 예외(`ApiException`). 서버 code·status 는 위 프로퍼티가
     *   나르지만, 원 호출 지점의 stack trace 는 이 값에만 남는다.
     * @property serverMessage 백엔드가 실제로 내려준 사용자 친화 message.
     *   **null 이면 서버가 message 미제공** — 호출처는 정적 R.string 으로 폴백한다. 클라가 만든 generic
     *   문구("알 수 없는 서버 에러" 등)는 여기 들어오지 않는다.
     *   null 일 때 `Throwable.message` 로 쓰는 문구는 Logcat·Crashlytics 단서용이라 노출하지 않는다.
     */
    class ServerRejection(
        val status: Int,
        val serverCode: Int,
        val serverMessage: String?,
        cause: Throwable,
    ) : ReceiverFailure(
            serverMessage ?: "receiver request rejected (status=$status, serverCode=$serverCode)",
            cause,
        )

    /**
     * 서버 응답 없이 전송 계층에서 끝난 실패(DNS 해석 불가·타임아웃·연결 거부 등)의 도메인 표현.
     *
     * [ServerRejection] 과 갈라 두는 이유 — 서버가 거절한 것이 아니라 **닿지도 못한** 것이라
     * `status`·`serverCode` 가 존재하지 않고, 화면 안내도 "연결을 확인하라" 로 갈린다.
     * presentation 은 core:network 에 의존하지 않으므로 이 타입 하나로 그 분기를 한다.
     *
     * 원인 예외는 `cause` 로 보존한다(로그 진단용) — 표시 문구로 쓰지 않는다. 영문 기술 원문
     * (`java.net.UnknownHostException: Unable to resolve host ...`)이 화면에 실리기 때문이다.
     */
    class NetworkUnavailable(
        cause: Throwable,
    ) : ReceiverFailure("receiver request failed before any response", cause)

    /**
     * 발신자가 세운 전달 조건이 아직 충족되지 않아 거절됐다는 사실
     * (BE `ErrorCode.DELIVERY_CONDITION_NOT_MET` — 403).
     *
     * [ServerRejection] 에서 갈라 **타입으로** 세운 이유 — 소비처가 화면 분기와 문구 노출을 모두
     * 이 사유에 걸어야 하는데, 그때마다 `serverCode == 2009` 를 보게 하면 BE 의 code 체계가
     * presentation 까지 샌다. 어느 번호였는지는 data 계층만 알면 된다.
     *
     * @property serverMessage 서버가 내려준 안내. 이 사유는 서버 문구 자체가 사용자 안내라
     *   소비처가 allowlist 를 다시 묻지 않고 그대로 쓴다. **null 이면 서버가 message 미제공** —
     *   그때는 호출처가 정적 리소스로 폴백한다.
     */
    class DeliveryConditionNotMet(
        val serverMessage: String?,
        cause: Throwable,
    ) : ReceiverFailure(
            serverMessage ?: "delivery condition not met",
            cause,
        )
}
