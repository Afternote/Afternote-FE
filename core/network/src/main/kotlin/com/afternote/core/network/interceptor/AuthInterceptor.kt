package com.afternote.core.network.interceptor

import com.afternote.core.domain.repository.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()

            // 1. 일단 원래 요청에 토큰을 실어서 보냄
            val token =
                runBlocking {
                    authRepository.getAccessToken()
                }
            val request =
                originalRequest
                    .newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()

            val response = chain.proceed(request)

            // 2. 만약 401(만료)이 떴다면?
            if (response.code == 401) {
                synchronized(this) {
                    // 3. 리이슈를 실행해서 토큰을 갈아치움 (EnsureSession 로직)
                    val refreshToken =
                        runBlocking {
                            authRepository.getRefreshToken()
                        } ?: return chain.proceed(chain.request())
                    val newToken =
                        runBlocking {
                            authRepository.rotateToken(
                                refreshToken = refreshToken,
                            )
                        }

                    if (newToken != null) {
                        response.close() // 기존 응답 닫기

                        // 4. 새 토큰으로 다시 요청 생성
                        val newRequest =
                            originalRequest
                                .newBuilder()
                                .header("Authorization", "Bearer $newToken")
                                .build()

                        return chain.proceed(newRequest) // 재시도!
                    }
                }
            }
            return response
        }
    }
