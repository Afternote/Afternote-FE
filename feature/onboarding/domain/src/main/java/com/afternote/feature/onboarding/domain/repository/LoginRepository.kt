package com.afternote.feature.onboarding.domain.repository

/**
 * 온보딩 로그인 도메인 Repository 인터페이스.
 * 카카오 액세스 토큰을 서버에 전달해 앱 토큰을 발급받고 저장합니다.
 */
interface LoginRepository {
    suspend fun loginWithKakao(kakaoAccessToken: String): Result<Unit>
}
