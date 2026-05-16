package com.afternote.core.network.interceptor

import com.afternote.core.network.model.ApiException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 4xx/5xx 응답의 `BaseResponse` 본문을 파싱해 [ApiException] 으로 throw 한다.
 *
 * Retrofit 기본 동작은 4xx 응답에 대해 `HttpException` 을 자동으로 throw 하여
 * 본문 파싱 단계까지 도달하지 못한다. 결과적으로 호출부의 `runCatching` 은
 * `HttpException` 만 받고, 백엔드가 보내준 `{"message":"..."}` 가 사용자(Snackbar)
 * 에게 전달되지 않는다.
 *
 * 본 인터셉터가 가장 외곽(가장 먼저 addInterceptor) 에서 응답을 받아 본문의
 * `message` 를 꺼내 [ApiException] 으로 변환한다.
 */
class ApiErrorInterceptor
    @Inject
    constructor(
        private val json: Json,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            if (response.code !in 400..599) return response

            val rawBody = response.peekBody(Long.MAX_VALUE).string()
            val parsed = runCatching { json.parseToJsonElement(rawBody).jsonObject }.getOrNull()
            val status = parsed?.get("status")?.jsonPrimitive?.intOrNull ?: response.code
            val code = parsed?.get("code")?.jsonPrimitive?.intOrNull ?: response.code
            val message =
                parsed
                    ?.get("message")
                    ?.jsonPrimitive
                    ?.content
                    ?.takeUnless { it.isBlank() }
                    ?: response.message.takeUnless { it.isBlank() }
                    ?: "요청에 실패했습니다."

            throw ApiException(
                status = status,
                code = code,
                message = message,
            )
        }
    }
