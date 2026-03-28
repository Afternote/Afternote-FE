package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
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
            runCatching {
                val session =
                    getSession(
                        authRepository = authRepository,
                        loginType = loginType,
                    )

                authRepository.saveSession(
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                    userId = session.userId,
                )
            }
    }

private suspend fun getSession(
    authRepository: AuthRepository,
    loginType: LoginType,
): Session =
    when (loginType) {
        is LoginType.Email -> {
            authRepository
                .defaultLogin(
                    email = loginType.email,
                    password = loginType.password,
                )
        }

        is LoginType.Kakao -> {
            authRepository.kakaoLogin()
        }
    }.getOrNull() ?: error("세션 획득 실패")
