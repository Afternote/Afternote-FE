package com.afternote.core.network.interceptor

import android.util.Log
import com.afternote.core.domain.repository.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator
    @Inject
    constructor(
        private val authRepository: dagger.Lazy<AuthRepository>,
    ) : Authenticator {
        @Suppress("ReturnCount")
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (responseCount(response) >= 3) {
                Log.e("TokenAuthenticator", "❌ 무한 루프 방지: 재시도 횟수 3회 이상. 세션 만료 처리")
                runBlocking { authRepository.get().clearSession() }
                return null
            }

            val originalRequest = response.request
            val authHeader = originalRequest.header("Authorization")
            val oldAccessToken =
                authHeader?.let {
                    if (it.startsWith("Bearer ", ignoreCase = true)) it.substring(7) else it
                }
            if (oldAccessToken == null) {
                Log.e("TokenAuthenticator", "❌ 인증 실패: 직전 요청이 애초에 토큰을 포함하지 않았음")
                return null
            }

            synchronized(this) {
                // 한 스레드만 접근할 수 있도록 락
                val currentAccessToken = runBlocking { authRepository.get().getAccessToken() }.getOrNull()
                // 앞선 다른 스레드가 토큰을 받아왔을 수도 있기 때문에 다시 현재 토큰을 확인
                if (oldAccessToken != currentAccessToken && !currentAccessToken.isNullOrEmpty()) {
                    return buildRequest(
                        request = originalRequest,
                        accessToken = currentAccessToken,
                    )
                }

                val tokenBundle =
                    runBlocking {
                        authRepository.get().rotateToken()
                    }.getOrNull()
                val newAccessToken = tokenBundle?.accessToken
                if (newAccessToken.isNullOrEmpty()) {
                    Log.e("TokenAuthenticator", "❌ 리이슈 실패: 세션 만료. 로그아웃 처리 진행")
                    runBlocking { authRepository.get().clearSession() }
                    return null
                }
                if (newAccessToken == oldAccessToken) {
                    Log.e("TokenAuthenticator", "❌ 리이슈 실패: 서버가 이전과 동일한 토큰을 반환함")
                    runBlocking { authRepository.get().clearSession() }
                    return null
                }
                return buildRequest(
                    request = originalRequest,
                    accessToken = newAccessToken,
                )
            }
        }
    }

private fun buildRequest(
    request: Request,
    accessToken: String,
) = request
    .newBuilder()
    .header("Authorization", "Bearer $accessToken")
    .build()

private fun responseCount(response: Response): Int {
    var count = 1
    var prior = response.priorResponse
    while (prior != null) {
        count++
        prior = prior.priorResponse
    }
    return count
}
