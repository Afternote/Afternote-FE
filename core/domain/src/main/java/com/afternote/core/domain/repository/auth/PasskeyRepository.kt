package com.afternote.core.domain.repository.auth

import com.afternote.core.model.PasskeyAuthenticationOptions
import com.afternote.core.model.Session

/**
 * 패스키(WebAuthn) **로그인** 계약.
 *
 * [AuthRepository] 에 얹지 않고 따로 세운 이유 — 그쪽은 세션 보관과 기존 로그인 수단을 담은
 * 계약이고 구현체·fake·테스트가 이미 여럿 물려 있다. 수단 하나를 얹자고 그 계약을 넓히면
 * 아직 패스키와 무관한 구현들이 전부 끌려온다. 세션 저장은 여기서 하지 않고
 * `PasskeyLoginUseCase` 가 [AuthRepository.saveSession] 으로 **기존 경로를 그대로 재사용**한다.
 */
interface PasskeyRepository {
    /**
     * 인증 옵션(challenge 포함)을 발급받는다. 로그인 이전이라 액세스 토큰 없이 호출한다.
     *
     * 옵션은 서버가 1회용으로 발급·소비하므로, 받아 둔 값을 재사용하지 말고 시도마다 새로 받는다.
     */
    suspend fun authenticationOptions(): Result<PasskeyAuthenticationOptions>

    /**
     * 인증기가 만든 assertion 을 서버에 검증시키고 세션을 발급받는다.
     *
     * @param assertionJson Credential Manager 가 돌려준 `PublicKeyCredential` JSON 원문.
     * @return 기존 이메일 로그인과 같은 [Session.DefaultSession] — 패스키에는 "이번에 가입됐다"
     *   개념이 없다(등록된 자격이 이미 있어야 인증이 성립한다). 그래서 소셜의
     *   `Session.SocialSession` 이 아니라 이쪽이다.
     */
    suspend fun authenticate(assertionJson: String): Result<Session.DefaultSession>
}
