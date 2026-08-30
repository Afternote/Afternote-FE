package com.afternote.core.model

/**
 * 패스키(WebAuthn) 인증 요청 옵션.
 *
 * @property requestJson 서버가 발급한 `PublicKeyCredentialRequestOptions` JSON 원문.
 *   Credential Manager `GetPublicKeyCredentialOption(requestJson)` 에 **그대로** 넣는다.
 *   앱이 해석해 쓸 필드가 없어 구조화하지 않고 문자열로 나른다 — challenge 는 서버가
 *   `clientDataJSON` 안의 값과 대조해 소비하므로, 중간에서 재조립할 이유가 없다.
 */
data class PasskeyAuthenticationOptions(
    val requestJson: String,
)
