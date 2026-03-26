package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.AuthRepository
import javax.inject.Inject

class GetUserInfoUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(): Long? = authRepository.getUserId()
    }
