package com.afternote.core.domain.repository.account

import com.afternote.core.model.EmailVerify
import com.afternote.core.model.SignUp

interface AccountRepository {
    suspend fun sendEmailCode(email: String): Result<Unit>

    suspend fun verifyEmail(
        email: String,
        certificateCode: String,
    ): Result<EmailVerify>

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        profileUrl: String?,
    ): Result<SignUp>

    suspend fun passwordChange(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit>
}
