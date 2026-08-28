package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendEmailCodeRequestDto(
    val email: String,
)

@Serializable
data class VerifyEmailRequestDto(
    val email: String,
    @SerialName("certificateCode") val certificateCode: String,
)

/** 아이디/비밀번호 찾기 전용 인증번호 발송 요청 (POST /auth/find/send/code). 회원가입용 [SendEmailCodeRequestDto] 와 엔드포인트가 다르다. */
@Serializable
data class FindSendCodeRequestDto(
    val email: String,
)

/**
 * 아이디/비밀번호 찾기 인증번호 발송 결과.
 *
 * [expiresAt]은 서버가 발급한 인증번호의 UTC 만료 절대시각(ISO-8601)이다. 네트워크 계층에서
 * 필수 응답 필드로 보존하고, 화면에서 카운트다운을 표시할지는 별도 제품 흐름이 결정한다.
 */
@Serializable
data class FindSendCodeDto(
    @SerialName("expiresAt") val expiresAt: String,
)

@Serializable
data class EmailFindRequestDto(
    val email: String,
    @SerialName("certificateCode") val certificateCode: String,
)

/** 아이디 찾기 결과. 이 앱의 로그인 아이디가 곧 [email] 이라 서버는 가입 이메일을 그대로 돌려준다. */
@Serializable
data class EmailFindDto(
    val name: String,
    val email: String,
)

/** 비밀번호 찾기/재설정 요청. 인증번호 검증과 새 비밀번호 반영을 한 요청에서 수행한다. */
@Serializable
data class PasswordFindRequestDto(
    val email: String,
    @SerialName("certificateCode") val certificateCode: String,
    val newPassword: String,
    val confirmPassword: String,
)

@Serializable
data class SignUpRequestDto(
    val email: String,
    val password: String,
    val name: String,
    @SerialName("profileUrl") val profileUrl: String? = null,
)

@Serializable
data class SignUpDto(
    @SerialName("userId") val userId: Long,
    val email: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

/**
 * Request for unified social login (POST /auth/social/login).
 *
 * @property provider 서버가 토큰을 어느 플랫폼에 검증하러 갈지 정하는 값 — `"KAKAO"` / `"GOOGLE"`.
 * @property accessToken **이름과 달리 provider 마다 종류가 다르다.** 카카오는 OAuth 액세스 토큰
 *   (`OAuthToken.accessToken`), 구글은 서명된 ID 토큰(`GoogleIdTokenCredential.idToken`) 이 들어간다.
 *   전자는 서버가 카카오 API 를 호출해야 신원을 알 수 있고, 후자는 서명 검증만으로 알 수 있다.
 *   필드명은 서버 계약이라 클라에서 바꿀 수 없다 — 해석 분기는 [provider] 가 맡는다.
 */
@Serializable
data class SocialLoginRequestDto(
    val provider: String,
    @SerialName("accessToken") val accessToken: String,
)

sealed class LoginDto {
    abstract val accessToken: String
    abstract val refreshToken: String

    /**
     * 액세스 토큰 잔여 수명(초). BE #410(2026-06-20)으로 발급 응답 `data` 안에 포함 —
     * RFC 6749 §5.1 의 `expires_in` 자리(access_token 형제)다. 선제 reissue(#408) deadline 의
     * 입력값으로, 토큰 저장 시점에 `AuthRepositoryImpl` 이 `AccessTokenExpiryTracker.record` 로
     * 기록한다. 서버가 생략하면(과거 호환) null — 그땐 기존 deadline 을 비워(stale 방지)
     * 401 사후 대응이 받는다.
     */
    abstract val expiresIn: Long?

    @Serializable
    data class DefaultLoginDto(
        override val accessToken: String,
        override val refreshToken: String,
        override val expiresIn: Long? = null,
    ) : LoginDto()

    @Serializable
    data class SocialLoginDto(
        override val accessToken: String,
        override val refreshToken: String,
        /**
         * 이번 로그인으로 계정이 새로 만들어졌는지. 온보딩(약관 동의) 진입 여부를 이 값 하나가 가른다.
         *
         * BE 는 primitive `boolean` 이라 항상 실려 온다. 기본값을 두지 않아 누락·오타가 조용한
         * "기존 유저" 취급으로 흡수되지 않게 한다 — 키가 `newUser` 로 어긋나 있던 3개월간(#993)
         * 이 분기가 통째로 죽어 있었고, nullable 기본값이 그 실패를 정상 동작처럼 보이게 했다.
         * 서버 키 이름은 `AuthDtoSocialLoginContractTest` 가 배포 스키마 기준으로 가드한다.
         */
        @SerialName("isNewUser") val isNewUser: Boolean,
        override val expiresIn: Long? = null,
    ) : LoginDto()
}

@Serializable
data class ReissueRequestDto(
    val refreshToken: String,
)

@Serializable
data class ReissueDto(
    val accessToken: String,
    val refreshToken: String,
    /** 재발급된 액세스 토큰의 잔여 수명(초). BE #410 으로 reissue 응답 `data` 에 포함. [LoginDto.expiresIn] 참고. */
    val expiresIn: Long? = null,
)

@Serializable
data class LogoutRequestDto(
    val refreshToken: String,
)

@Serializable
data class PasswordChangeRequestDto(
    val currentPassword: String,
    val newPassword: String,
)
