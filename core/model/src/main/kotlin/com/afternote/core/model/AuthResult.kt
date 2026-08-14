package com.afternote.core.model

/**
 * 회원가입 성공 결과. (스웨거: userId)
 */
data class AccountRegistration(
    val userId: Long,
    val email: String,
)

/**
 * 아이디 찾기 성공 결과.
 *
 * @property email 가입 이메일. 이 앱은 이메일과 별개인 아이디를 두지 않아(로그인 = 이메일 + 비밀번호)
 *   찾은 "아이디" 가 곧 이 값이다.
 */
data class FoundAccount(
    val name: String,
    val email: String,
)

/**
 * 로그인 성공 결과.
 */
sealed class Session {
    abstract val accessToken: String
    abstract val refreshToken: String

    data class DefaultSession(
        override val accessToken: String,
        override val refreshToken: String,
    ) : Session()

    data class SocialSession(
        override val accessToken: String,
        override val refreshToken: String,
        val isNewUser: Boolean?,
    ) : Session()
}

/**
 * 토큰 재발급 성공 결과.
 *
 * @property expiresIn 재발급된 액세스 토큰의 잔여 수명(초). BE #410 으로 reissue 응답에 포함 —
 *   선제 reissue(#408)의 deadline 입력값으로 `TokenReissuer` 가 회전 성공 시 기록한다.
 *   서버가 생략하면 null (그땐 기록을 비워 401 사후 대응에 맡긴다).
 */
data class TokenBundle(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long? = null,
)
