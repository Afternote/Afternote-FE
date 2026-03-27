package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.auth.AuthRepository
import javax.inject.Inject

sealed class LoginType {
    data class Email(
        val email: String,
        val password: String,
    ) : LoginType()

    object Kakao : LoginType()
}

class LoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(loginType: LoginType) =
            when (loginType) {
                is LoginType.Email -> {
                    authRepository
                        .login(
                            email = loginType.email,
                            password = loginType.password,
                        )
                }

                is LoginType.Kakao -> {
                    authRepository.kakaoLogin()
                }
            }
    }
