package com.afternote.core.domain.usecase.account

import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.SignUp
import javax.inject.Inject

/**
 * 회원가입 UseCase.
 */
class SignUpUseCase
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
            name: String,
            profileUrl: String?,
        ): Result<SignUp> = accountRepository.signUp(email, password, name, profileUrl)
    }
