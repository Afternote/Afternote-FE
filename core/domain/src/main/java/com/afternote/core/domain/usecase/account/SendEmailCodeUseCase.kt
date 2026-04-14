package com.afternote.core.domain.usecase.account

import com.afternote.core.domain.repository.account.AccountRepository
import javax.inject.Inject

/**
 * 이메일 인증번호 발송 UseCase.
 */
class SendEmailCodeUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(email: String): Result<Unit> = accountRepository.sendEmailCode(email)
    }
