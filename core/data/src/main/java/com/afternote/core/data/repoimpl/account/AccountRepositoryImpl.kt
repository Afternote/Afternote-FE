package com.afternote.core.data.repoimpl.account

import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.core.network.dto.EmailFindRequestDto
import com.afternote.core.network.dto.FindSendCodeRequestDto
import com.afternote.core.network.dto.PasswordChangeRequestDto
import com.afternote.core.network.dto.SendEmailCodeRequestDto
import com.afternote.core.network.dto.SignUpRequestDto
import com.afternote.core.network.dto.VerifyEmailRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.AccountApiService
import javax.inject.Inject

// TODO:리팩토링해야 하는지 검사 필요
class AccountRepositoryImpl
    @Inject
    constructor(
        private val accountApiService: AccountApiService,
    ) : AccountRepository {
        override suspend fun sendEmailCode(email: String): Result<Unit> =
            runCatching {
                accountApiService.sendEmailCode(SendEmailCodeRequestDto(email))
            }

        override suspend fun verifyEmail(
            email: String,
            certificateCode: String,
        ): Result<Unit> =
            runCatching {
                accountApiService
                    .verifyEmail(
                        VerifyEmailRequestDto(
                            email,
                            certificateCode,
                        ),
                    ).requireStatus()
            }

        override suspend fun sendFindCode(email: String): Result<Unit> =
            runCatching {
                accountApiService
                    .sendFindCode(FindSendCodeRequestDto(email))
                    .requireStatus()
            }

        override suspend fun findAccount(
            email: String,
            certificateCode: String,
        ): Result<FoundAccount> =
            runCatching {
                val response =
                    accountApiService.findEmail(
                        EmailFindRequestDto(
                            email,
                            certificateCode,
                        ),
                    )
                AuthMapper.toFoundAccount(response.requireData())
            }

        override suspend fun signUp(
            email: String,
            password: String,
            name: String,
            profileUrl: String?,
        ): Result<AccountRegistration> =
            runCatching {
                val response =
                    accountApiService.signUp(
                        SignUpRequestDto(
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
                accountApiService
                    .passwordChange(
                        PasswordChangeRequestDto(
                            currentPassword,
                            newPassword,
                        ),
                    ).requireStatus()
            }
    }
