package com.afternote.core.domain.usecase.account

import com.afternote.core.domain.repository.account.AccountRepository
import javax.inject.Inject

/**
 * 비밀번호 변경 UseCase.
 */
class PasswordChangeUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> = accountRepository.passwordChange(currentPassword, newPassword)
    }
