package com.afternote.core.domain.error

/**
 * 카카오톡 수신자 초대(발급·조회·수락) 흐름의 실패 어휘 (#944).
 *
 * Data 계층이 서버 code 를 이 계열로 번역하고, presentation 은 타입만 보고 안내와 토큰 처분을 정한다.
 * BE `ErrorCode` 번호는 Data 계층 밖으로 나오지 않는다(`ReceiverFailure` 와 같은 규약).
 *
 * @param message 리포팅 콘솔용 정적 진단 문구 — 화면에 싣지 않는다.
 * @param cause 이 실패를 만든 인프라 예외. stack trace 보존용이다.
 */
sealed class ReceiverInvitationFailure(
    message: String,
    cause: Throwable,
) : Exception(message, cause) {
    /** 초대가 없다(404·1905). 토큰을 지우고 안내로 끝낸다. */
    class NotFound(
        cause: Throwable,
    ) : ReceiverInvitationFailure("receiver invitation not found", cause)

    /** 초대가 만료됐다(410·1906). 토큰을 지우고 안내로 끝낸다. */
    class Expired(
        cause: Throwable,
    ) : ReceiverInvitationFailure("receiver invitation expired", cause)

    /** 다른 사용자가 이미 수락한 초대다(409·1907). 토큰을 지우고 안내로 끝낸다. */
    class AcceptedByOther(
        cause: Throwable,
    ) : ReceiverInvitationFailure("receiver invitation accepted by another user", cause)

    /** 초대를 만든 본인이 수락하려 했다(400·1908). 토큰을 지우고 안내로 끝낸다. */
    class SelfAccept(
        cause: Throwable,
    ) : ReceiverInvitationFailure("receiver invitation self accept", cause)

    /** 이미 그 초대자의 수신자로 등록돼 있다(409·1909). 토큰을 지우고 받은 기록함으로 보낸다. */
    class AlreadyRegistered(
        cause: Throwable,
    ) : ReceiverInvitationFailure("receiver already registered for inviter", cause)

    /** 로그인 세션이 없거나 만료됐다(401·1000). 토큰은 남기고 로그인으로 보낸다. */
    class Unauthenticated(
        cause: Throwable,
    ) : ReceiverInvitationFailure("receiver invitation requires authentication", cause)

    /**
     * 위 어느 사유로도 확정되지 않은 실패.
     *
     * @property isRetryable 서버에 닿지 못했거나(IOException) 5xx 라 같은 토큰으로 다시 시도할 수 있다.
     *   이 갈래에서는 토큰을 지우지 않는다. 미등재 4xx 는 false — 다시 보내도 같은 거절이 온다.
     */
    class Other(
        cause: Throwable,
        val isRetryable: Boolean,
    ) : ReceiverInvitationFailure("receiver invitation failed: retryable=$isRetryable", cause)
}
