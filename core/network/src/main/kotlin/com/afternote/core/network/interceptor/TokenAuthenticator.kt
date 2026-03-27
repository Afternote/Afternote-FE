package com.afternote.core.network.interceptor

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
        val authRepository: AuthRepository,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            val originalRequest = response.request
            val header = originalRequest.header("Authorization")
            val oldAccessToken = header?.removePrefix("Bearer ")

            // 2. 만약 401(만료)이 떴다면?
            synchronized(this) {
                val currentAccessToken = runBlocking { authRepository.getAccessToken() }.getOrNull()
                if (oldAccessToken != currentAccessToken && !currentAccessToken.isNullOrEmpty()) {
                    return getRequest(
                        originalRequest = originalRequest,
                        accessToken = currentAccessToken,
                    )
                }

                // 3. 리이슈를 실행해서 토큰을 갈아치움 (EnsureSession 로직)
                val refreshToken =
                    runBlocking {
                        authRepository.getRefreshToken()
                    }.getOrNull() ?: return null
                val tokenBundle =
                    runBlocking {
                        authRepository.rotateToken(
                            refreshToken = refreshToken,
                        )
                    }.getOrNull()
                val newAccessToken = tokenBundle?.accessToken
                if (!newAccessToken.isNullOrEmpty() || newAccessToken == oldAccessToken) {
                    return null
                }
                return getRequest(
                    originalRequest = originalRequest,
                    accessToken = newAccessToken,
                )
            }
        }
    }

private fun getRequest(
    originalRequest: Request,
    accessToken: String?,
): Request {
    val requestBuilder = originalRequest.newBuilder()
    requestBuilder.header("Authorization", "Bearer $accessToken")
    return requestBuilder.build()
}
