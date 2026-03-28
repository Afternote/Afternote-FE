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

            val accessToken =
                // 블록 내의 작업이 끝날 때까지 너(runBlocking을 호출한 스레드)는 이 작업에서 벗어나지 마
                runBlocking {
                    authRepository.getAccessToken()
                }.getOrNull()

            // 액세스 토큰이 없으면
            if (accessToken.isNullOrEmpty()) {
                // 그냥 바로 다음 인터셉터한테 요청 넘기고 응답 받은 다음에 꺼져라
                return chain.proceed(originalRequest)
            }

            // 액세스 토큰이 있으면 가기 전에 헤더에 달고 나가라
            val authenticatedRequest =
                originalRequest
                    .newBuilder() // 엄마가 가방 매 줄게
                    .header("Authorization", "Bearer $accessToken") // 가방에 헤더 넣고
                    .build() // 다 맸다

            return chain.proceed(authenticatedRequest)
        }
    }
