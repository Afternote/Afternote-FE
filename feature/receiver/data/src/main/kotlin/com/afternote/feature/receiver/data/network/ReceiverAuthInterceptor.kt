package com.afternote.feature.receiver.data.network

import com.afternote.feature.receiver.data.local.ReceiverMasterKeyDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * `/receiver-auth/` 경로로 나가는 모든 요청에 [ReceiverMasterKeyDataSource]에 저장된
 * 인증 코드를 `X-Auth-Code` 헤더로 자동 부착한다. 코드가 없으면 헤더 없이 통과시켜
 * 서버가 401을 반환하도록 둔다.
 */
class ReceiverAuthInterceptor
    @Inject
    constructor(
        private val masterKeyDataSource: ReceiverMasterKeyDataSource,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (!request.url.encodedPath.contains(RECEIVER_AUTH_PATH_SEGMENT)) {
                return chain.proceed(request)
            }
            val code = runBlocking { masterKeyDataSource.savedCodeFlow.first() }
            if (code.isNullOrBlank()) {
                return chain.proceed(request)
            }
            val authenticated =
                request
                    .newBuilder()
                    .header("X-Auth-Code", code)
                    .build()
            return chain.proceed(authenticated)
        }

        private companion object {
            const val RECEIVER_AUTH_PATH_SEGMENT = "/receiver-auth/"
        }
    }
