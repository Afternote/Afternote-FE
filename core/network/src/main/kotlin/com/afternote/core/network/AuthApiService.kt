package com.afternote.core.network

import com.afternote.core.network.dto.LoginData
import com.afternote.core.network.dto.LoginRequest
import com.afternote.core.network.dto.LogoutRequest
import com.afternote.core.network.dto.PasswordChangeRequest
import com.afternote.core.network.dto.ReissueData
import com.afternote.core.network.dto.ReissueRequest
import com.afternote.core.network.dto.SendEmailCodeRequest
import com.afternote.core.network.dto.SignUpData
import com.afternote.core.network.dto.SignUpRequest
import com.afternote.core.network.dto.SocialLoginRequest
import com.afternote.core.network.dto.VerifyEmailData
import com.afternote.core.network.dto.VerifyEmailRequest
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/email/send")
    suspend fun sendEmailCode(
        @Body body: SendEmailCodeRequest,
    ): BaseResponse<Unit>

    @POST("auth/email/verify")
    suspend fun verifyEmail(
        @Body body: VerifyEmailRequest,
    ): BaseResponse<VerifyEmailData>

    @POST("auth/sign-up")
    suspend fun signUp(
        @Body body: SignUpRequest,
    ): BaseResponse<SignUpData>

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest,
    ): BaseResponse<LoginData>

    @POST("auth/social/login")
    suspend fun socialLogin(
        @Body body: SocialLoginRequest,
    ): BaseResponse<LoginData>

    @POST("auth/reissue")
    suspend fun reissue(
        @Body body: ReissueRequest,
    ): BaseResponse<ReissueData>

    @POST("auth/logout")
    suspend fun logout(
        @Body body: LogoutRequest,
    ): BaseResponse<Unit>

    @POST("auth/password/change")
    suspend fun passwordChange(
        @Body body: PasswordChangeRequest,
    ): BaseResponse<Unit>
}
