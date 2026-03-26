package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.AuthRepository
import javax.inject.Inject

sealed class LoginType {
    data class Email(
        val email: String,
        val password: String,
    ) : LoginType()

    data class Kakao(
        val socialAccessToken: String,
    ) : LoginType()
}

class LoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            password: String,
            socialAccessToken: String,
        ) {
            authRepository.login(
                email = email,
                password = password,
            )
            authRepository.kakaoLogin(
                socialAccessToken = socialAccessToken,
            )
        }
    }
