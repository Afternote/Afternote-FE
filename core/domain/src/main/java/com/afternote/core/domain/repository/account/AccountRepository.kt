package com.afternote.core.domain.repository.account

import com.afternote.core.model.EmailVerifyResult
import com.afternote.core.model.SignUpResult

interface AccountRepository {
    suspend fun sendEmailCode(email: String): Result<Unit>

    suspend fun verifyEmail(
        email: String,
        certificateCode: String,
    ): Result<EmailVerifyResult>

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        profileUrl: String?,
    ): Result<SignUpResult>

    suspend fun passwordChange(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit>
}
