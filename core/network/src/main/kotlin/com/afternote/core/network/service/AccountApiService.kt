package com.afternote.core.network.service

import com.afternote.core.network.dto.EmailFindData
import com.afternote.core.network.dto.EmailFindRequest
import com.afternote.core.network.dto.FindSendCodeRequest
import com.afternote.core.network.dto.PasswordChangeRequest
import com.afternote.core.network.dto.PasswordFindRequest
import com.afternote.core.network.dto.SendEmailCodeRequest
import com.afternote.core.network.dto.SignUpData
import com.afternote.core.network.dto.SignUpRequest
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
    ): BaseResponse<Unit>

    @POST("auth/sign-up")
    suspend fun signUp(
        @Body body: SignUpRequest,
    ): BaseResponse<SignUpData>

    @POST("auth/password/change")
    suspend fun passwordChange(
        @Body body: PasswordChangeRequest,
    ): BaseResponse<Unit>

    // 아이디/비밀번호 찾기 인증번호 발송
    @POST("auth/find/send/code")
    suspend fun sendFindCode(
        @Body body: FindSendCodeRequest,
    ): BaseResponse<Unit>

    // 아이디(이메일) 찾기
    @POST("auth/email/find")
    suspend fun findEmail(
        @Body body: EmailFindRequest,
    ): BaseResponse<EmailFindData>

    // 비밀번호 찾기/재설정
    @POST("auth/password/find")
    suspend fun findPassword(
        @Body body: PasswordFindRequest,
    ): BaseResponse<Unit>
}
