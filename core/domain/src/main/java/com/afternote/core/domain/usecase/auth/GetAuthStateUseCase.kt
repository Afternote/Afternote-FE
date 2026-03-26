package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAuthStateUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        operator fun invoke(): Flow<Boolean> = authRepository.isLoggedIn
    }
