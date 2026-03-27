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
            val accessToken =
                runBlocking {
                    authRepository.getAccessToken()
                }.getOrNull()

            if (accessToken.isNullOrEmpty()) {
                return chain.proceed(originalRequest)
            }

            val requestBuilder = originalRequest.newBuilder()
            requestBuilder.header("Authorization", "Bearer $accessToken")
            val request = requestBuilder.build()

            return chain.proceed(request)
        }
    }
