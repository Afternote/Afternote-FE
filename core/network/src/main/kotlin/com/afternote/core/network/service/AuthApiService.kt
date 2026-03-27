package com.afternote.core.network.service

import com.afternote.core.network.dto.LoginData
import com.afternote.core.network.dto.LoginRequest
import com.afternote.core.network.dto.LogoutRequest
import com.afternote.core.network.dto.SocialLoginData
import com.afternote.core.network.dto.SocialLoginRequest
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest,
    ): BaseResponse<LoginData>

    @POST("auth/social/login")
    suspend fun socialLogin(
        @Body body: SocialLoginRequest,
    ): BaseResponse<SocialLoginData>

    @POST("auth/logout")
    suspend fun logout(
        @Body body: LogoutRequest,
    ): BaseResponse<Unit>
}
