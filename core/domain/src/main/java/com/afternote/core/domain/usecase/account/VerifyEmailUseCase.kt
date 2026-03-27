package com.afternote.core.domain.usecase.account

import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.EmailVerify
import javax.inject.Inject

/**
 * 이메일 인증번호 확인 UseCase.
 */
class VerifyEmailUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            certificateCode: String,
        ): Result<EmailVerify> = accountRepository.verifyEmail(email, certificateCode)
    }
