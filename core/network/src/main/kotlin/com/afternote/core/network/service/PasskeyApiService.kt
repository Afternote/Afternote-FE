package com.afternote.core.network.service

import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.PasskeyAuthenticateRequestDto
import com.afternote.core.network.dto.PasskeyAuthenticationOptionsDto
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 패스키(WebAuthn) **로그인** 엔드포인트 (BE#133).
 *
 * 등록(`register/options`·`register`)과 관리(`users/passkeys`)는 담당이 갈린 #765 몫이라
 * 여기 두지 않는다 — 이 계약은 로그인 갈래만 담는다.
 */
interface PasskeyApiService {
    /**
     * 인증 옵션 발급. 서버가 challenge 를 만들어 저장하고 표준 옵션 객체를 돌려준다.
     *
     * **비로그인 호출이다** — BE `WhiteListUrl` 에 등록돼 있어 액세스 토큰 없이 통과한다.
     * 본문도 받지 않는다(BE 컨트롤러에 `@RequestBody` 가 없다).
     */
    @POST("auth/passkey/authenticate/options")
    suspend fun authenticateOptions(): BaseResponse<PasskeyAuthenticationOptionsDto>

    /**
     * assertion 검증 후 로그인. 성공 응답은 **기존 이메일 로그인과 같은 토큰 봉투**다 —
     * BE `PasskeyService.authenticate` 가 `authService.issueTokens(...)` 를 그대로 타서
     * `LoginResponse`(accessToken·refreshToken·expiresIn)를 돌려주므로 [LoginDto.DefaultLoginDto]
     * 를 재사용한다.
     */
    @POST("auth/passkey/authenticate")
    suspend fun authenticate(
        @Body body: PasskeyAuthenticateRequestDto,
    ): BaseResponse<LoginDto.DefaultLoginDto>
}
