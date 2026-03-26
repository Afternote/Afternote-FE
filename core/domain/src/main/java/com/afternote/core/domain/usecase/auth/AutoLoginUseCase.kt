package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.AuthRepository
import javax.inject.Inject

class AutoLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        operator fun invoke() = authRepository.isLoggedIn
    }
