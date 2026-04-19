package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import javax.inject.Inject

sealed class LoginType {
    data class Email(
        val email: String,
        val password: String,
    ) : LoginType()

    data class Kakao(
        val oauthToken: String,
    ) : LoginType()

    data class Google(
        val idToken: String,
    ) : LoginType()
}

class LoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        suspend operator fun invoke(loginType: LoginType): Result<Unit> {
            val sessionResult: Result<Session> =
                when (loginType) {
                    is LoginType.Email -> {
                        authRepository.defaultLogin(
                            email = loginType.email,
                            password = loginType.password,
                        )
                    }

                    is LoginType.Kakao -> {
                        authRepository.kakaoLogin(oauthToken = loginType.oauthToken)
                    }

                    is LoginType.Google -> {
                        authRepository.googleLogin(idToken = loginType.idToken)
                    }
                }

            val session =
                sessionResult.getOrElse { exception ->
                    return Result.failure(exception)
                }

            return authRepository.saveSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                userId = session.userId,
            )
        }
    }
