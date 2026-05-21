package com.afternote.feature.timeletter.data.network

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject

class TimeLetterDebugMockNetworkInterceptor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Interceptor {
        private val isMockEnabled: Boolean
            get() =
                context
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(KEY_MOCK_ENABLED, true)

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            if (!isMockEnabled) return chain.proceed(request)

            val path = request.url.encodedPath
            val method = request.method

            val json =
                when {
                    path == "/api/v1/time-letters" && method == "GET" -> {
                        TimeLetterMockFixtures.MOCK_LIST_JSON
                    }

                    path == "/api/v1/time-letters/temporary" && method == "GET" -> {
                        TimeLetterMockFixtures.MOCK_TEMPORARY_LIST_JSON
                    }

                    path == "/api/v1/time-letters" && method == "POST" -> {
                        TimeLetterMockFixtures.MOCK_CREATE_JSON
                    }

                    path == "/api/v1/time-letters/delete" && method == "POST" -> {
                        TimeLetterMockFixtures.MOCK_DELETE_JSON
                    }

                    path == "/api/v1/time-letters/temporary" && method == "DELETE" -> {
                        TimeLetterMockFixtures.MOCK_DELETE_JSON
                    }

                    path.matches(TIME_LETTER_ID_PATH_REGEX) && method == "GET" -> {
                        val id = path.substringAfterLast('/').toLongOrNull() ?: 1L
                        TimeLetterMockFixtures.detailJson(id)
                    }

                    path.matches(TIME_LETTER_ID_PATH_REGEX) && method == "PATCH" -> {
                        TimeLetterMockFixtures.MOCK_UPDATE_JSON
                    }

                    else -> {
                        return chain.proceed(request)
                    }
                }

            Log.w(TAG, "⚠️ MOCK 응답 반환: $method $path")
            Thread.sleep(NETWORK_DELAY_MS)

            return Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(HTTP_OK)
                .message("OK")
                .body(json.toResponseBody(JSON_MEDIA_TYPE))
                .build()
        }

        private companion object {
            const val TAG = "TimeLetterMockInterceptor"
            const val HTTP_OK = 200
            const val NETWORK_DELAY_MS = 300L
            const val PREFS_NAME = "debug_mock_mode"
            const val KEY_MOCK_ENABLED = "mock_enabled"
            val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
            val TIME_LETTER_ID_PATH_REGEX = Regex("^/api/v1/time-letters/\\d+$")
        }
    }
