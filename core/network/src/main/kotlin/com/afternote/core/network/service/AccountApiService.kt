package com.afternote.core.network.service

import com.afternote.core.network.dto.PasswordChangeRequestDto
import com.afternote.core.network.dto.SendEmailCodeRequestDto
import com.afternote.core.network.dto.SignUpDto
import com.afternote.core.network.dto.SignUpRequestDto
import com.afternote.core.network.dto.VerifyEmailRequestDto
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AccountApiService {
    @POST("auth/email/send")
    suspend fun sendEmailCode(
        @Body body: SendEmailCodeRequestDto,
    ): BaseResponse<Unit>

    @POST("auth/email/verify")
    suspend fun verifyEmail(
        @Body body: VerifyEmailRequestDto,
    ): BaseResponse<Unit>

    @POST("auth/sign-up")
    suspend fun signUp(
        @Body body: SignUpRequestDto,
    ): BaseResponse<SignUpDto>

    @POST("auth/password/change")
    suspend fun passwordChange(
        @Body body: PasswordChangeRequestDto,
    ): BaseResponse<Unit>
}
