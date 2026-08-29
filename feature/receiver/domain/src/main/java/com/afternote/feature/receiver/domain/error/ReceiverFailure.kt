package com.afternote.feature.receiver.domain.error

/**
 * 수신자 흐름(본인 확인 이메일 인증·마스터 키 검증·전달 자격 심사)의 실패 중 **사유가 확인된 것**의
 * 도메인 루트. Retrofit·`BaseResponse` 같은 인프라 디테일은 Data 계층이 해석한 뒤 이 계열로 통일한다.
 *
 * 소비처는 이 루트로 좁힌 뒤 `when` 으로 가른다 — 실패 유형이 늘면 컴파일러가 소비처를 잡아준다.
 * 서버 응답과 네트워크 실패는 Data 계층이 이 계열로 번역한다. 그 밖의 로컬 실패는 원본 그대로
 * 흘려보내므로, 소비처의 «루트가 아닌 Throwable» 분기는 계속 필요하다.
 *
 * @param message **리포팅 콘솔용 정적 진단 문구.** 화면에 싣지 않는다 — 표시 문구는 사유별 매핑이
 *   갖는다. non-null 로 좁혀 하위 타입이 빠뜨릴 수 없게 했다: 비우면 `Exception` 이
 *   `cause.toString()`(`java.net.UnknownHostException: Unable to resolve host ...` 같은 영문 기술
 *   원문)을 message 로 앉힌다.
 * @param cause 이 실패를 만든 인프라 예외. non-null 인 건 이 계열이 전부 그런 예외를 옮겨
 *   만들어져 원인 없는 실패가 존재하지 않아서다. 버리면 원 호출 지점의 stack trace 가 끊겨
 *   Crashlytics 에서 «어디서 난 실패인지» 를 잃는다. (`CoreAuthFailure` 루트가 nullable 인 건
 *   사용자 취소처럼 예외에서 유래하지 않는 갈래를 갖기 때문이고, 이 계열엔 그런 갈래가 없다.)
 */
sealed class ReceiverFailure(
    message: String,
    cause: Throwable,
) : Exception(message, cause) {
    /**
     * 서버가 예상하고 처리한 사용자 거절.
     *
     * [reason] 은 화면에 별도 안내가 필요한 사유만 채운다. `null` 은 서버가 사용자 거절임을 설명했지만
     * FE 가 표시 사유로 등재하지 않았다는 뜻이다. 둘 다 정상적인 사용자 거절이므로 텔레메트리에서는
     * 제외하지만, 후자는 서버 원문을 화면에 싣지 않고 호출처 폴백으로 내린다.
     *
     * HTTP status·BE code·서버 message 는 Data 계층이 이 타입을 만들 때만 해석한다. 이 타입이 그 값을
     * 다시 운반하지 않아 presentation 이 인프라 규약을 되짚지 않게 한다.
     */
    class UserRejection(
        val reason: ReceiverRejectionReason?,
        cause: Throwable,
    ) : ReceiverFailure(
            "receiver request rejected by user input: ${reason?.name ?: "UNCLASSIFIED"}",
            cause,
        )

    /**
     * 서버가 응답했지만 예상한 사용자 거절로 확인할 수 없는 실패.
     *
     * 5xx 는 서버 문구가 있어도 장애이고, 등재되지 않은 code 의 문구 없는 4xx 도 FE 버그·계약 불일치
     * 신호일 수 있다. 둘은
     * 화면에서 정적 폴백을 사용하고 텔레메트리에 기록한다. 어느 대역·code 였는지는 [cause] 에만 남아
     * 진단에는 쓸 수 있지만 도메인 계약으로 노출되지는 않는다.
     */
    class UnexpectedServerFailure(
        cause: Throwable,
    ) : ReceiverFailure("unexpected receiver server failure", cause)

    /**
     * 서버 응답 없이 전송 계층에서 끝난 실패(DNS 해석 불가·타임아웃·연결 거부 등).
     *
     * [UnexpectedServerFailure] 와 갈라 두는 이유 — 서버가 거절한 것이 아니라 **닿지도 못한** 것이라
     * 화면 안내도 "연결을 확인하라" 로 갈린다.
     * presentation 은 core:network 에 의존하지 않으므로 이 타입 하나로 그 분기를 한다.
     */
    class NetworkUnavailable(
        cause: Throwable,
    ) : ReceiverFailure("receiver request failed before any response", cause)

    /**
     * 발신자가 세운 전달 조건이 아직 충족되지 않아 거절됐다는 사실.
     *
     * 일반 [UserRejection] 에서 갈라 **타입으로** 세운 이유 — 소비처가 화면 분기와 문구 노출을 모두
     * 이 사유에 걸어야 하는데, 그때마다 사유 code 를 비교하게 하면 BE 의 code 체계가 presentation
     * 까지 샌다. 어느 번호였는지는 Data 계층(`ReceiverFailureTranslation`)만 안다.
     *
     * **서버 문구를 싣지 않는다.** 사유가 타입으로 특정된 이상 표시 문구는 호출처 리소스가 가지면
     * 되고, 그쪽이 [com.afternote.core.domain.error.CoreAuthFailure] 의 규약이다(BE#92 — 서버
     * `message` 는 사용자 노출용이라는 규정이 없다). BE 쪽 문구도 `ErrorCode` enum 상수라 실을 만한
     * 동적 정보가 없다.
     */
    class DeliveryConditionNotMet(
        cause: Throwable,
    ) : ReceiverFailure("delivery condition not met", cause)
}

/** 화면에 별도 안내가 필요한 수신자 거절 사유. 서버 code 와 문구는 Data 계층이 이 어휘로 번역한다. */
enum class ReceiverRejectionReason {
    INVALID_AUTH_CODE,
    RECEIVER_EMAIL_NOT_FOUND,
    RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND,
    RECEIVER_EMAIL_AUTH_CODE_MISMATCH,
    VERIFICATION_ALREADY_SUBMITTED,
}
