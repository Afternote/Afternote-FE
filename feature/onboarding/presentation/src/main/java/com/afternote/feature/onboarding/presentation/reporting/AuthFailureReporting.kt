package com.afternote.feature.onboarding.presentation.reporting

import com.afternote.core.common.reporting.ErrorReporter

/**
 * 인증 흐름에서 실패가 발생한 지점.
 *
 * [reportingName] 은 리포팅 콘솔의 필터 값이다. 바꾸면 그 시점 이후 기록이 기존 이슈 그룹과
 * 끊겨 추이를 잃으므로, 단계 자체가 사라지지 않는 한 유지한다.
 */
enum class AuthFailureStage(
    val reportingName: String,
) {
    /** 소셜 SDK 에서 토큰을 받아오는 단계 — 서버 호출 이전. */
    SOCIAL_TOKEN_REQUEST("social_token_request"),

    /** 획득한 자격증명으로 서버 로그인을 호출하는 단계. */
    LOGIN("login"),

    /** 회원가입 이메일 인증번호 발송. */
    EMAIL_CODE_SEND("email_code_send"),

    /** 회원가입 이메일 인증번호 검증 — 인증번호 불일치·만료는 제외(사용자 입력 오류). */
    EMAIL_VERIFY("email_verify"),

    /**
     * 아이디 찾기 인증번호 발송 — 회원가입용 `EMAIL_CODE_SEND` 와 엔드포인트가 달라 따로 센다.
     *
     * 짝이 되는 "인증번호 확인"(`auth/email/find`)은 계측하지 않는다. 그 응답은 인증번호 오타와
     * 서버 장애가 같은 예외로 와서 클라이언트가 구분할 수단이 없고, 오타까지 기록하면
     * 보관 한도(최근 8건)를 사용자 오류가 차지한다. 서버가 사유를 구분해 주면 그때 추가한다.
     */
    FIND_ACCOUNT_CODE_SEND("find_account_code_send"),

    /**
     * 비밀번호 찾기 최종 제출(`auth/password/find`) — 인증번호 검증과 재설정을 겸한다.
     *
     * 인증번호 무효(1207)까지 함께 세는 것이 [FIND_ACCOUNT_CODE_SEND] KDoc 의 "확인은 계측하지
     * 않는다" 와 어긋나 보이지만, 여기서는 코드가 이미 자릿수를 채워 한 화면을 통과한 뒤라
     * 오타보다 만료·서버 문제일 확률이 높다. 재설정이 조용히 실패하면 사용자가 계정을 되찾지
     * 못하므로 실패 자체를 놓치지 않는 쪽을 택했다.
     */
    FIND_PASSWORD_RESET("find_password_reset"),

    /** 회원가입 최종 제출. */
    SIGN_UP("sign_up"),

    /** 가입 성공 직후의 자동 로그인 — 여기서 깨지면 가입은 됐는데 로그인 화면으로 되돌아간다. */
    AUTO_LOGIN_AFTER_SIGN_UP("auto_login_after_sign_up"),
}

/** 로그인 수단. 어느 경로에서 실패가 몰리는지 구분한다. */
enum class AuthProvider(
    val reportingName: String,
) {
    EMAIL("email"),
    KAKAO("kakao"),
    GOOGLE("google"),
}

/**
 * 인증 흐름의 handled 실패를 공통 키 규격으로 기록한다.
 *
 * 사용자 취소처럼 오류가 아닌 경로는 호출부에서 걸러 넘기지 않는다.
 *
 * @param provider 수단 구분이 의미 없는 단계(회원가입 등)에서는 생략한다.
 */
fun ErrorReporter.recordAuthFailure(
    stage: AuthFailureStage,
    throwable: Throwable,
    provider: AuthProvider? = null,
) {
    recordFailure(
        throwable = throwable,
        attributes =
            buildMap {
                put(KEY_AUTH_STAGE, stage.reportingName)
                provider?.let { put(KEY_AUTH_PROVIDER, it.reportingName) }
            },
    )
}

private const val KEY_AUTH_STAGE = "auth_stage"
private const val KEY_AUTH_PROVIDER = "auth_provider"
