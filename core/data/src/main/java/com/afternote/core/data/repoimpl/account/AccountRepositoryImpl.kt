package com.afternote.core.data.repoimpl.account

import com.afternote.core.common.result.runCatchingCancellable
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

internal class AccountRepositoryImpl
    @Inject
    constructor(
        private val accountApiService: AccountApiService,
    ) : AccountRepository {
        override suspend fun sendEmailCode(email: String): Result<Unit> =
            runCatchingCancellable {
                accountApiService
                    .sendEmailCode(SendEmailCodeRequestDto(email))
                    .requireStatus()
            }.mapAccountFailure()

        override suspend fun verifyEmail(
            email: String,
            certificateCode: String,
        ): Result<Unit> =
            runCatchingCancellable {
                accountApiService
                    .verifyEmail(
                        VerifyEmailRequestDto(
                            email,
                            certificateCode,
                        ),
                    ).requireStatus()
            }.mapAccountFailure()

        override suspend fun sendFindCode(email: String): Result<Unit> =
            runCatchingCancellable {
                accountApiService
                    .sendFindCode(FindSendCodeRequestDto(email))
                    .requireStatus()
            }.mapAccountFailure()

        override suspend fun findAccount(
            email: String,
            certificateCode: String,
        ): Result<FoundAccount> =
            runCatchingCancellable {
                val response =
                    accountApiService.findEmail(
                        EmailFindRequestDto(
                            email,
                            certificateCode,
                        ),
                    )
                AuthMapper.toFoundAccount(response.requireData())
            }.mapAccountFailure()

        override suspend fun signUp(
            email: String,
            password: String,
            name: String,
            profileUrl: String?,
        ): Result<AccountRegistration> =
            runCatchingCancellable {
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
            }.mapAccountFailure()

        override suspend fun passwordChange(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> =
            runCatchingCancellable {
                accountApiService
                    .passwordChange(
                        PasswordChangeRequestDto(
                            currentPassword,
                            newPassword,
                        ),
                    ).requireStatus()
            }
    }
