package com.afternote.core.data.repoimpl.account

import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.data.repoimpl.account.AccountRepositoryImpl.Companion.CODE_INVALID_VERIFICATION
import com.afternote.core.domain.error.EmailVerificationException
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.core.network.dto.EmailFindRequestDto
import com.afternote.core.network.dto.FindSendCodeRequestDto
import com.afternote.core.network.dto.PasswordChangeRequestDto
import com.afternote.core.network.dto.SendEmailCodeRequestDto
import com.afternote.core.network.dto.SignUpRequestDto
import com.afternote.core.network.dto.VerifyEmailRequestDto
import com.afternote.core.network.model.ApiException
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
        companion object {
            /**
             * 인증번호 무효(불일치/만료/미존재 통합) 서버 code.
             * 2026-07-16 curl 실측 — `POST auth/email/verify` 에 무효 코드 전송 시
             * HTTP 400 + `{"status":400,"code":1207,"message":"인증번호가 유효하지 않습니다."}`.
             */
            private const val CODE_INVALID_VERIFICATION = 1207
        }

        override suspend fun sendEmailCode(email: String): Result<Unit> =
            runCatching {
                accountApiService.sendEmailCode(SendEmailCodeRequestDto(email))
            }

        /**
         * 안쪽 catch 가 [ApiException](인프라 타입) 중 [CODE_INVALID_VERIFICATION] 만
         * [EmailVerificationException](도메인 타입)으로 바꿔 던지고, 바깥 runCatching 이 잡아
         * `Result.failure(도메인 예외)` 로 반환한다 — 그 외 실패는 원본 그대로 유지
         * (feature:afternote 의 ReceiverAuthRepositoryImpl 과 같은 exception translation 구조).
         *
         * 1207 만 구분하는 이유: 인증번호 무효는 사용자가 재입력으로 고칠 수 있는 실패라 호출처
         * (SignUpViewModel)가 인라인 에러로 보여주고, 그 외(네트워크·서버 장애)는 입력과 무관해
         * 스낵바로 보낸다 — 인증번호 무효도 그 외 요청 오류도 같은 HTTP 400 이라 상태코드로는
         * 두 부류가 안 갈리므로, 서버 봉투의 `code` 가 유일한 판별 신호다.
         */
        override suspend fun verifyEmail(
            email: String,
            certificateCode: String,
        ): Result<Unit> =
            runCatching {
                try {
                    accountApiService
                        .verifyEmail(
                            VerifyEmailRequestDto(
                                email,
                                certificateCode,
                            ),
                        ).requireStatus()
                } catch (e: ApiException) {
                    if (e.code == CODE_INVALID_VERIFICATION) {
                        throw EmailVerificationException(serverMessage = e.serverMessage, serverCode = e.code)
                    }
                    throw e
                }
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
