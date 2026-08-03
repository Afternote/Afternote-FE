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

class AccountRepositoryImpl
    @Inject
    constructor(
        private val accountApiService: AccountApiService,
    ) : AccountRepository {
        // requireStatus() 는 형제 메서드와 맞춘 것이다 — 이것만 빠져 있어 HTTP 200 봉투 안의
        // 실패 status 가 성공으로 통과했고, 그러면 실패를 옮길 매퍼가 볼 실패 자체가 생기지 않는다.
        override suspend fun sendEmailCode(email: String): Result<Unit> =
            runCatchingCancellable {
                accountApiService
                    .sendEmailCode(SendEmailCodeRequestDto(email))
                    .requireStatus()
            }.mapAccountFailure()

        /**
         * 인증번호 무효(서버 code 1207)를 `EmailVerificationException` 으로 갈라내는 일은
         * `mapAccountFailure` 가 형제 메서드와 똑같이 처리한다 — 여기서 따로 catch 하던 것을 걷었다.
         * 같은 판정을 두 곳에 두면 갈라지기 때문이다(실제로 blank 처리가 서로 어긋나 있었다).
         *
         * 1207 을 구분하는 이유: 인증번호 무효는 사용자가 재입력으로 고칠 수 있는 실패라 호출처가
         * 인라인 에러로 보여주고, 그 외(네트워크·서버 장애)는 입력과 무관해 스낵바로 보낸다.
         * 둘 다 같은 HTTP 400 이라 상태코드로는 안 갈리므로 서버 봉투의 `code` 가 유일한 판별 신호다.
         */
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
