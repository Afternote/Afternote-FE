package com.afternote.feature.onboarding.domain.usecase

import com.afternote.feature.onboarding.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * 카카오 로그인 UseCase.
 * Kakao SDK 에서 발급받은 액세스 토큰을 서버에 전달합니다.
 */
class LoginWithKakaoUseCase
    @Inject
    constructor(
        private val loginRepository: LoginRepository,
    ) {
        suspend operator fun invoke(kakaoAccessToken: String): Result<Unit> =
            loginRepository.loginWithKakao(kakaoAccessToken)
    }
