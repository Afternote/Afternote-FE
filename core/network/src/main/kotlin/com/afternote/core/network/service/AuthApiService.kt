package com.afternote.core.network.service

import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.LoginRequestDto
import com.afternote.core.network.dto.LogoutRequestDto
import com.afternote.core.network.dto.PasskeyDto
import com.afternote.core.network.dto.SocialLoginRequestDto
import com.afternote.core.network.model.BaseResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequestDto,
    ): BaseResponse<LoginDto.DefaultLoginDto>

    @POST("auth/social/login")
    suspend fun socialLogin(
        @Body body: SocialLoginRequestDto,
    ): BaseResponse<LoginDto.SocialLoginDto>

    @POST("auth/logout")
    suspend fun logout(
        @Body body: LogoutRequestDto,
    ): BaseResponse<Unit>

    /**
     * 패스키 등록용 challenge 옵션 조회 — 응답을 그대로(가공 없이) Android Credential Manager의
     * `CreatePublicKeyCredentialRequest.requestJson` 에 전달한다. WebAuthn 스펙 필드가 많고 서버
     * 구현 세부에 따라 달라질 수 있어 Kotlin DTO로 모델링하지 않고 원본 JSON을 그대로 왕복시킨다.
     */
    @POST("auth/passkey/register/options")
    suspend fun getPasskeyRegisterOptions(): BaseResponse<JsonElement>

    /**
     * Credential Manager가 반환한 등록 응답(`CreatePublicKeyCredentialResponse.registrationResponseJson`)을
     * 가공 없이 그대로 전달한다.
     */
    @POST("auth/passkey/register")
    suspend fun registerPasskey(
        @Body credential: JsonElement,
    ): BaseResponse<PasskeyDto>
}
