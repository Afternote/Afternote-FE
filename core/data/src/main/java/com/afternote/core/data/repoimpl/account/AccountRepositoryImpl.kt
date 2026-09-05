package com.afternote.core.data.repoimpl.account

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.core.network.dto.EmailFindRequestDto
import com.afternote.core.network.dto.FindSendCodeRequestDto
import com.afternote.core.network.dto.PasswordChangeRequestDto
import com.afternote.core.network.dto.PasswordFindRequestDto
import com.afternote.core.network.dto.SendEmailCodeRequestDto
import com.afternote.core.network.dto.SignUpRequestDto
import com.afternote.core.network.dto.VerifyEmailRequestDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.AccountApiService
import java.io.IOException
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

        override suspend fun resetPassword(
            email: String,
            certificateCode: String,
            newPassword: String,
            confirmPassword: String,
        ): Result<Unit> =
            runCatchingCancellable {
                accountApiService
                    .findPassword(
                        PasswordFindRequestDto(
                            email = email,
                            certificateCode = certificateCode,
                            newPassword = newPassword,
                            confirmPassword = confirmPassword,
                        ),
                    ).requireStatus()
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

private const val CODE_INVALID_VERIFICATION = 1207
private const val CODE_EMAIL_ALREADY_REGISTERED = 1200
private const val CODE_NEW_PASSWORD_UNCHANGED = 1206
private const val CODE_SOCIAL_LOGIN_USER = 1702

/**
 * 계정 API 실패를 도메인 예외로 옮긴다 — presentation 이 `core:network` 를 모른 채 타입만으로
 * 분기하게 하는 것이 목적이다(#646).
 *
 * 가르는 신호는 서버 봉투의 `code` 뿐이고 `message` 는 옮기지 않는다(BE#92). 표시 문구는 각 화면이
 * 자기 리소스로 갖는다. 취소는 다시 보지 않는다 — 호출부가 전부 `runCatchingCancellable`(#661)
 * 이라 `CancellationException` 이 [Result] 로 도달하지 않는다.
 */
private fun <T> Result<T>.mapAccountFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is ApiException -> {
            when (exception.code) {
                CODE_INVALID_VERIFICATION -> {
                    Result.failure(CoreAuthFailure.EmailVerification(exception))
                }

                CODE_EMAIL_ALREADY_REGISTERED -> {
                    Result.failure(CoreAuthFailure.EmailAlreadyRegistered(exception))
                }

                CODE_NEW_PASSWORD_UNCHANGED -> {
                    Result.failure(CoreAuthFailure.PasswordUnchanged(exception))
                }

                CODE_SOCIAL_LOGIN_USER -> {
                    Result.failure(CoreAuthFailure.SocialSignUpAccount(exception))
                }

                else -> {
                    this
                }
            }
        }

        is IOException -> {
            Result.failure(CoreAuthFailure.NetworkUnavailable(exception))
        }

        else -> {
            this
        }
    }
