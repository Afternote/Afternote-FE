package com.afternote.feature.afternote.data.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject

/**
 * DEBUG 빌드에서만 컴파일됩니다. Retrofit 공용 [okhttp3.OkHttpClient]에 연결되어
 * 애프터노트·음악 검색 HTTP를 실서버로 보내지 않고 고정 JSON으로 응답합니다.
 *
 * 릴리즈에서는 이 타입이 클래스패스에 없고, 멀티바인딩 Set도 비어 있습니다.
 *
 * [mockModePreferences]가 꺼져 있으면 모든 요청을 실서버로 통과시킵니다.
 *
 * TODO: [MOCK_CLEANUP] 백엔드 API 완성 후 불필요한 엔드포인트 목업을 순차 제거할 것.
 */
class AfternoteDebugMockNetworkInterceptor
    @Inject
    constructor(
        private val mockModePreferences: MockModePreferences,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()

            if (!mockModePreferences.isEnabled) return chain.proceed(request)

            val path = request.url.encodedPath
            val method = request.method

            val json =
                when {
                    path == "/auth/social/login" && method == "POST" -> MOCK_SOCIAL_LOGIN_JSON
                    path == "/auth/login" && method == "POST" -> MOCK_LOGIN_JSON
                    path == "/auth/logout" && method == "POST" -> MOCK_LOGOUT_JSON

                    path == "/music/search" && method == "GET" -> MOCK_MUSIC_SEARCH_JSON
                    path == "/api/afternotes" && method == "GET" -> MOCK_LIST_JSON
                    path == "/api/afternotes" && method == "POST" -> MOCK_CREATE_JSON
                    path.matches(AFTERNOTE_ID_PATH_REGEX) ->
                        when (method) {
                            "GET" -> mockDetailJson(path)
                            "PATCH" -> MOCK_PATCH_JSON
                            "DELETE" -> MOCK_DELETE_JSON
                            else -> return chain.proceed(request)
                        }
                    else -> return chain.proceed(request)
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

        private fun mockDetailJson(path: String): String {
            val id = path.substringAfterLast('/').toLongOrNull() ?: 1L
            return detailJsonForId(id)
        }

        private companion object {
            const val TAG = "MockInterceptor"
            const val HTTP_OK = 200
            const val NETWORK_DELAY_MS = 500L
            val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
            val AFTERNOTE_ID_PATH_REGEX = Regex("^/api/afternotes/\\d+$")

            fun detailJsonForId(id: Long): String =
                """
                {"status":200,"code":0,"message":null,"data":{"afternoteId":$id,"category":"SOCIAL","title":"[목업] 애프터노트 상세","createdAt":"2024-01-01","updatedAt":"2024-01-02","credentials":null,"receivers":[],"processMethod":null,"actions":[],"leaveMessage":null,"playlist":null}}
                """.trimIndent()

            val MOCK_LIST_JSON =
                """
                {"status":200,"code":0,"message":null,"data":{"content":[{"afternoteId":1,"title":"[목업] 소셜 서비스","category":"SOCIAL","createdAt":"2024-06-01"},{"afternoteId":2,"title":"[목업] 갤러리","category":"GALLERY","createdAt":"2024-06-02"}],"pageNumber":0,"size":10,"hasNext":false}}
                """.trimIndent()

            val MOCK_CREATE_JSON =
                """
                {"status":200,"code":0,"message":null,"data":{"afternoteId":99}}
                """.trimIndent()

            val MOCK_PATCH_JSON =
                """
                {"status":200,"code":0,"message":null,"data":{"afternoteId":99}}
                """.trimIndent()

            val MOCK_DELETE_JSON =
                """
                {"status":200,"code":0,"message":null,"data":{"afternoteId":1}}
                """.trimIndent()

            val MOCK_SOCIAL_LOGIN_JSON =
                """
                {"status":200,"code":0,"message":null,"data":{"accessToken":"mock_access_token","refreshToken":"mock_refresh_token","userId":1,"isNewUser":false}}
                """.trimIndent()

            val MOCK_LOGIN_JSON =
                """
                {"status":200,"code":0,"message":null,"data":{"accessToken":"mock_access_token","refreshToken":"mock_refresh_token","userId":1}}
                """.trimIndent()

            val MOCK_LOGOUT_JSON =
                """
                {"status":200,"code":0,"message":null,"data":null}
                """.trimIndent()

            val MOCK_MUSIC_SEARCH_JSON =
                """
                {"tracks":[{"artist":"목업 아티스트","title":"목업 곡 제목","albumImageUrl":null}]}
                """.trimIndent()
        }
    }
