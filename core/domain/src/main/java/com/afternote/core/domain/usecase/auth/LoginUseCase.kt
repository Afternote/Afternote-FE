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
        /**
         * 로그인 후 세션을 저장한다. 반환 [Boolean] 은 "온보딩이 필요한 소셜 신규 가입자"인지 —
         * 소셜 로그인 `newUser == true` 만 `true`, 이메일·기존 유저는 `false` (`newUser` 가 null 이어도 false).
         */
        suspend operator fun invoke(loginType: LoginType): Result<Boolean> {
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

            return authRepository
                .saveSession(
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                ).map {
                    // 소셜 로그인이고 서버가 newUser=true 를 준 경우만 신규. 이메일(cast→null)·false·null 은 `== true` 로 모두 false.
                    (session as? Session.SocialSession)?.isNewUser == true
                }
        }
    }
