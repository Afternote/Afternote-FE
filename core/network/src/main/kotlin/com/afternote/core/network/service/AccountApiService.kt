package com.afternote.core.network.service

import com.afternote.core.network.dto.PasswordChangeRequest
import com.afternote.core.network.dto.SendEmailCodeRequest
import com.afternote.core.network.dto.SignUpData
import com.afternote.core.network.dto.SignUpRequest
import com.afternote.core.network.dto.VerifyEmailData
import com.afternote.core.network.dto.VerifyEmailRequest
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AccountApiService {
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

    @POST("auth/password/change")
    suspend fun passwordChange(
        @Body body: PasswordChangeRequest,
    ): BaseResponse<Unit>
}
