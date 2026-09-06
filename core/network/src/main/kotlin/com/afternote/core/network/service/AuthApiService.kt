package com.afternote.core.network.service

import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.LoginRequestDto
import com.afternote.core.network.dto.LogoutRequestDto
import com.afternote.core.network.dto.SocialLoginRequestDto
import com.afternote.core.network.model.BaseResponse
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
}
