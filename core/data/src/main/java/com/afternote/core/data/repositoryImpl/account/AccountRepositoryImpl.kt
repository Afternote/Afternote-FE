package com.afternote.core.data.repositoryImpl.account

import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.EmailVerify
import com.afternote.core.model.SignUp
import com.afternote.core.network.dto.PasswordChangeRequest
import com.afternote.core.network.dto.SendEmailCodeRequest
import com.afternote.core.network.dto.SignUpRequest
import com.afternote.core.network.dto.VerifyEmailData
import com.afternote.core.network.dto.VerifyEmailRequest
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.AccountApiService
import javax.inject.Inject

// TODO:리팩토링해야 하는지 검사 필요
class AccountRepositoryImpl
    @Inject
    constructor(
        val accountApiService: AccountApiService,
    ) : AccountRepository {
        override suspend fun sendEmailCode(email: String): Result<Unit> =
            runCatching {
                accountApiService.sendEmailCode(SendEmailCodeRequest(email))
            }

        override suspend fun verifyEmail(
            email: String,
            certificateCode: String,
        ): Result<EmailVerify> =
            runCatching {
                val response =
                    accountApiService.verifyEmail(
                        VerifyEmailRequest(
                            email,
                            certificateCode,
                        ),
                    )
                response.requireStatus()

                AuthMapper.toEmailVerifyResult(response.data ?: VerifyEmailData(isVerified = null))
            }

        override suspend fun signUp(
            email: String,
            password: String,
            name: String,
            profileUrl: String?,
        ): Result<SignUp> =
            runCatching {
                val response =
                    accountApiService.signUp(
                        SignUpRequest(
                            email,
                            password,
                            name,
                            profileUrl,
                        ),
                    )
                AuthMapper.toSignUpResult(response.requireData())
            }

        override suspend fun passwordChange(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> =
            runCatching {
                val response =
                    accountApiService.passwordChange(
                        PasswordChangeRequest(
                            currentPassword,
                            newPassword,
                        ),
                    )
            }
    }
