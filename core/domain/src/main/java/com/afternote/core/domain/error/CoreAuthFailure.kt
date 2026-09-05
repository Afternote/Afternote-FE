package com.afternote.core.domain.error

/**
 * 인증·계정 흐름(로그인·회원가입·이메일 인증·소셜 연동)의 실패 중 **사유가 확인된 것**의 공통 루트.
 *
 * data 계층이 `ApiException` 을 이 계열로 번역하고, presentation 은 이 루트로 좁힌 뒤 `when` 으로
 * 가른다 — 하위 타입이 늘면 컴파일러가 소비처를 잡아준다(`sealed`). 사유를 확인하지 못한 실패는
 * 번역하지 않고 원본 그대로 흘려보내므로, 소비처의 `else`(루트가 아닌 Throwable) 분기는 계속 필요하다.
 *
 * 표시 문구는 호출처 리소스가 갖는다(BE#92 — 서버 `message` 는 사용자 노출용이라는 규정이 없다).
 * 서버 코드값은 cause 인 `ApiException` 이 갖는다. `message` 는 그 둘 어느 쪽도 아닌 **리포팅 콘솔용
 * 정적 진단 문구**이며, 파라미터를 non-null 로 좁혀 하위 타입이 빠뜨릴 수 없게 했다 — 기본 생성에 맡기면
 * `cause.toString()`(`java.net.UnknownHostException: Unable to resolve host ...` 같은 영문 기술 원문)이
 * message 가 된다. 이 값은 화면에 싣지 않는다(`e.message ?: 폴백` 금지) — 표시 문구는 사유별 매핑이
 * 갖고, 사유를 확인하지 못한 실패는 호출처가 넘긴 작업별 폴백 리소스로 내려앉힌다.
 */
sealed class CoreAuthFailure(
    message: String,
    cause: Throwable?,
) : Exception(message, cause) {
    /**
     * 이메일 로그인 자격 거절(서버 code 1201·1202). 계정 미존재와 비밀번호 불일치를 가르지 않는다 —
     * 서버 문구도 시안(`3628:23437`)도 어느 쪽이 틀렸는지 노출하지 않는 단일 문구다.
     */
    class InvalidLoginCredentials(
        cause: Throwable,
    ) : CoreAuthFailure("login credentials rejected", cause)

    /**
     * 소셜 로그인 거절(서버 code 1208·1209). [InvalidLoginCredentials] 와 갈라 두는 이유 —
     * 입력 필드와 무관한 실패라 화면이 필드 인라인이 아닌 별도 안내로 표시해야 한다.
     */
    class SocialLoginRejected(
        cause: Throwable,
    ) : CoreAuthFailure("social login rejected", cause)

    /** 이미 가입된 이메일로 인증코드 발송을 요청했다는 사실(서버 code 1200). */
    class EmailAlreadyRegistered(
        cause: Throwable,
    ) : CoreAuthFailure("email already registered", cause)

    /** 이메일 인증번호가 무효(불일치·만료·미존재 — 서버는 code 1207 하나로 내려준다)라는 사실. */
    class EmailVerification(
        cause: Throwable,
    ) : CoreAuthFailure("email verification code invalid", cause)

    /**
     * 그 이메일이 소셜 로그인으로 가입한 계정이라는 사실(서버 code 1702).
     *
     * BE `AuthService` 는 `password == null` 인 사용자를 **세 경로**에서 이 코드로 거절한다 —
     * 계정 복구(`findActiveLocalUserForRecovery`) · 이메일 로그인(`login`) · 비밀번호 변경
     * (`passwordChange`). 복구는 인증번호 발송(`auth/find/send/code`) 단계에서 이미 걸리므로,
     * 비밀번호 찾기 화면은 코드 입력 전에 차단 안내를 낼 수 있다. 온보딩 전용 사유가 아니다.
     *
     * [SocialLoginRejected] 와 다르다 — 그쪽은 소셜 로그인 **시도**가 거절된 것이고, 이쪽은 로컬
     * 비밀번호가 없는 계정에 비밀번호를 요구한 것이라 안내가 "소셜로 로그인하라" 로 갈린다.
     */
    class SocialSignUpAccount(
        cause: Throwable,
    ) : CoreAuthFailure("account signed up via social provider", cause)

    /**
     * 새 비밀번호가 지금 쓰는 비밀번호와 같다는 사실(서버 code 1206).
     *
     * 서버만 판정할 수 있다 — 클라는 기존 비밀번호를 모른다(비밀번호 찾기는 현재 비밀번호를
     * 입력받지 않는다). 폴백 문구로 뭉개면 "왜 실패했는지" 를 사용자가 알 수 없어 따로 둔다.
     */
    class PasswordUnchanged(
        cause: Throwable,
    ) : CoreAuthFailure("new password equals current password", cause)

    /**
     * 서버 응답 없이 전송 계층에서 끝난 실패(DNS 해석 불가·타임아웃·연결 거부 등)의 도메인 표현.
     *
     * data 계층이 인증·계정 API 호출의 IO 예외를 이 타입으로 치환한다 — presentation 은
     * core:network 에 의존하지 않으므로, 이 타입 하나로 "네트워크 연결 안내" 분기를 할 수 있다.
     * 원인 예외는 [cause] 로 보존한다(로그 진단용).
     */
    class NetworkUnavailable(
        cause: Throwable,
    ) : CoreAuthFailure("network unavailable", cause)

    /**
     * 사용자가 소셜 로그인·계정 연동 인증을 직접 취소했다는 사실.
     *
     * 코루틴의 [kotlinx.coroutines.CancellationException] 과 구분해 구조화된 동시성(Structured
     * Concurrency)이 깨지지 않도록 별도 타입으로 둔다 — 취소를 그 타입으로 나르면 상위 스코프까지
     * 취소로 전파된다. 소비처는 이 타입을 리포팅·에러 표시에서 제외하고 조용히 흘려보낸다.
     *
     * 서버가 관여하지 않는 단말 측 이탈이라 [cause] 가 없다.
     */
    class UserCancelledAuth : CoreAuthFailure("user cancelled social auth", null)
}
