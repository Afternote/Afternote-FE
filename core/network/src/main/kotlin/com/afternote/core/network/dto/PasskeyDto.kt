package com.afternote.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 패스키 인증 옵션 응답 (`POST auth/passkey/authenticate/options`).
 *
 * 필드와 nullable 판정 근거는 **BE 실코드**다 — `PasskeyService.authenticateOptions()` 가
 * builder 로 다섯 값을 전부 무조건 채우고(`challenge`·`timeout`·`rpId`·`allowCredentials`·
 * `userVerification`), 어느 갈래에서도 생략하지 않는다. 그래서 전부 non-null 로 선언한다.
 * OpenAPI 문서는 전 필드를 optional 로 보이게 하므로 근거로 쓰지 않았다.
 *
 * 이 응답은 그대로 WebAuthn `PublicKeyCredentialRequestOptions` 의 모양이다 — 서버가 앱을 위해
 * 따로 감싼 봉투가 아니라, 표준 옵션 객체를 공통 응답의 `data` 에 실어 보내는 것이다.
 */
@Serializable
data class PasskeyAuthenticationOptionsDto(
    /** base64url challenge. 서버가 발급 시 저장해 두고 검증 때 1회 소비한다. */
    val challenge: String,
    /** 옵션 유효 시간(ms). BE 상수 `OPTIONS_TIMEOUT_MS` = 300_000. */
    val timeout: Long,
    /** Relying Party ID. BE `passkey.rp-id` 설정값. */
    val rpId: String,
    /**
     * 사용할 수 있는 자격 목록.
     *
     * **BE 는 지금 항상 빈 배열을 보낸다** — usernameless(사용자 식별 없이 기기의 패스키를 고르는)
     * 로그인이라 후보를 좁히지 않는다(`PasskeyService.authenticateOptions()` 의 `List.of()`).
     * 그래도 형을 세워 두는 이유는, 서버가 나중에 후보를 채우기 시작해도 조용히 버려지지 않게
     * 하려는 것이다 — 여기서 지워진 후보는 시스템 선택기에서 그대로 사라진다.
     */
    val allowCredentials: List<PasskeyCredentialDescriptorDto>,
    /** 사용자 검증 요구 수준. BE 는 `"required"` 고정. */
    val userVerification: String,
)

/** [PasskeyAuthenticationOptionsDto.allowCredentials] 의 원소 — WebAuthn `PublicKeyCredentialDescriptor`. */
@Serializable
data class PasskeyCredentialDescriptorDto(
    val type: String,
    val id: String,
)

/**
 * 패스키 인증 검증 요청 (`POST auth/passkey/authenticate`).
 *
 * @property credential Credential Manager 가 돌려준 `PublicKeyCredential` assertion JSON을
 *   **파싱만 하고 손대지 않은 트리**로 싣는다. 서버는 이 값을 다시 문자열로 만들어
 *   webauthn4j 에 넘기고(`PasskeyService.credentialJson`), 그 안의 `clientDataJSON` ·
 *   `signature` 는 인증기가 서명한 바이트라 필드를 하나라도 재조립하면 검증이 깨진다.
 *   BE 는 `credential` 로 감싼 형태와 최상위 assertion 을 모두 받지만, 감싼 쪽이
 *   "이 봉투에 무엇이 들었는지" 를 이름으로 말해 주므로 그쪽을 쓴다.
 */
@Serializable
data class PasskeyAuthenticateRequestDto(
    val credential: JsonObject,
)
