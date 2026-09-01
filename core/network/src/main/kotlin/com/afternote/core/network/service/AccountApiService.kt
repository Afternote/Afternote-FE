package com.afternote.core.network.service

import com.afternote.core.network.dto.EmailFindDto
import com.afternote.core.network.dto.EmailFindRequestDto
import com.afternote.core.network.dto.FindSendCodeDto
import com.afternote.core.network.dto.FindSendCodeRequestDto
import com.afternote.core.network.dto.PasswordChangeRequestDto
import com.afternote.core.network.dto.PasswordFindRequestDto
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

    @POST("auth/find/send/code")
    suspend fun sendFindCode(
        @Body body: FindSendCodeRequestDto,
    ): BaseResponse<FindSendCodeDto>

    @POST("auth/email/find")
    suspend fun findEmail(
        @Body body: EmailFindRequestDto,
    ): BaseResponse<EmailFindDto>

    @POST("auth/password/find")
    suspend fun findPassword(
        @Body body: PasswordFindRequestDto,
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
