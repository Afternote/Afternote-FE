package com.afternote.core.network.interceptor

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * `https://mock.image/{seed}` 요청을 Lorem Picsum으로 치환한다. 도메인 데이터는 가짜 URL만 유지하고
 * 실제 바이트는 네트워크 단에서만 바꾼다.
 *
 * 애플리케이션 인터셉터에서 307을 반환하면 OkHttp 리다이렉트가 적용되지 않으므로 [chain.proceed]로 URL만 바꾼다.
 */
class ImageMockInterceptor
    @Inject
    constructor() : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url
            if (url.host != MOCK_IMAGE_HOST) return chain.proceed(request)

            val seed = url.pathSegments.lastOrNull()?.takeIf { it.isNotBlank() } ?: "1"
            val picsumUrl = "https://picsum.photos/seed/$seed/400/600".toHttpUrl()
            val newRequest = request.newBuilder().url(picsumUrl).build()
            return chain.proceed(newRequest)
        }

        private companion object {
            const val MOCK_IMAGE_HOST = "mock.image"
        }
    }
