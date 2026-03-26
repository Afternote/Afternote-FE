package com.afternote.core.network.interceptor

import com.afternote.core.datastore.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp 인터셉터 — 모든 요청에 Authorization: Bearer {accessToken} 헤더를 추가합니다.
 *
 * DataStore 는 Flow 기반 비동기 API 만 제공하므로 runBlocking 을 사용합니다.
 * OkHttp 는 이미 별도의 IO 스레드풀에서 실행되므로 메인 스레드를 차단하지 않습니다.
 */
class AuthInterceptor
    @Inject
    constructor(
        private val tokenManager: TokenManager,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val accessToken = runBlocking { tokenManager.getAccessToken() }
            val request =
                chain
                    .request()
                    .newBuilder()
                    .apply {
                        if (accessToken != null) {
                            addHeader("Authorization", "Bearer $accessToken")
                        }
                    }.build()
            return chain.proceed(request)
        }
    }
