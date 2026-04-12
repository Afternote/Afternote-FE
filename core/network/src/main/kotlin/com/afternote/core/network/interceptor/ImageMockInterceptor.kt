package com.afternote.core.network.interceptor

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * `https://mock.image/{slot}` 요청을 Lorem Picsum 고정 ID 이미지로 치환한다. (400×600 유지)
 *
 * 경로의 숫자는 **슬롯 번호**로만 쓰이고, 실제 다운로드 URL은 엄선한 ID 목록을 순환해 고정되므로
 * Coil 디스크/메모리 캐시 적중률이 높다. [chain.proceed]로 URL만 바꾼다.
 */
class ImageMockInterceptor
    @Inject
    constructor() : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url = request.url
            if (url.host != MOCK_IMAGE_HOST) return chain.proceed(request)

            val slot =
                url.pathSegments
                    .lastOrNull()
                    ?.toIntOrNull()
                    ?.coerceAtLeast(1) ?: 1
            val picId = BEAUTIFUL_PIC_IDS[(slot - 1) % BEAUTIFUL_PIC_IDS.size]
            val picsumUrl = "https://picsum.photos/id/$picId/400/600".toHttpUrl()
            val newRequest = request.newBuilder().url(picsumUrl).build()
            return chain.proceed(newRequest)
        }

        private companion object {
            const val MOCK_IMAGE_HOST = "mock.image"

            /** 고정 이미지 ID — 매 요청마다 바뀌는 seed 대신 캐시 친화적. */
            private val BEAUTIFUL_PIC_IDS =
                listOf(
                    "1015",
                    "1016",
                    "1018",
                    "1025",
                    "1032",
                    "1043",
                    "1050",
                    "1060",
                    "1074",
                    "1084",
                )
        }
    }
