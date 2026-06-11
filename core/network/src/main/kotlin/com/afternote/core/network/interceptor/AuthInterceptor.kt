package com.afternote.core.network.interceptor

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.token.AccessTokenExpiryTracker
import com.afternote.core.network.token.TokenReissuer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 액세스 토큰을 요청 헤더에 부착하고, 토큰 수명을 관리하는 인터셉터.
 *
 * 부착 외에 두 가지 횡단 처리(#408)를 함께 한다 — 둘 다 "유효한 토큰을 부착한다"는 같은 관심사:
 * 1. 요청 전: [AccessTokenExpiryTracker] 기준 잔여 수명이 임계값 미만이면 선제 reissue.
 *    회전은 [TokenReissuer] 단일 락 경유라 401 경로(`TokenAuthenticator`)와 이중 실행되지
 *    않는다. 실패해도 기존 토큰으로 진행 — 401 사후 대응이 안전망이고, 선제 경로는 clearSession
 *    하지 않는다(일시 오류로 세션을 날리면 안 됨). reissue 요청 자체는 토큰 미부착 `RefreshClient`
 *    의 별도 Retrofit 을 타므로 재귀 없음.
 * 2. 응답 후: 봉투의 `expiresIn`(잔여 수명 초, 일부 목록 endpoint 만 내려줌)을 피킹해 기록.
 *
 * 알려진 한계(의도적 수용): 로그아웃 요청이 만료 임박 창(60초)에 걸리면 선제 회전 후 본문의
 * 구 refresh 로 로그아웃이 나가 새 refresh 가 서버에 잔존할 수 있다 — 발생 창이 좁고 로컬 토큰은
 * 항상 정리되므로 endpoint 별 예외 경로를 두지 않는다.
 */
class AuthInterceptor
    @Inject
    constructor(
        // authRepository.get()이 호출되는 시점으로 AuthRepository 생성 늦춤
        private val authRepository: dagger.Lazy<AuthRepository>,
        private val expiryTracker: AccessTokenExpiryTracker,
        private val tokenReissuer: TokenReissuer,
        private val json: Json,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()

            val storedToken =
                // 블록 내의 작업이 끝날 때까지 너(runBlocking을 호출한 스레드)는 이 작업에서 벗어나지 마
                // — intercept 는 Response 를 즉시 반환해야 하는 동기 콜백이라 suspend 결과를 "기다려서
                // 받아야" 한다. 발사만 하고 떠나는 launch/async 는 여기서 못 쓰고, 이게 공식 문서가
                // 명시한 runBlocking 용도("non-suspend callbacks when suspend functions need to be called")
                runBlocking {
                    authRepository.get().getAccessToken()
                }.getOrNull()

            // 액세스 토큰이 없으면
            if (storedToken.isNullOrEmpty()) {
                // 이전 토큰 기준 deadline 은 stale — tracker 는 in-memory 라 로그아웃이 지워 주지 않아,
                // 안 씻으면 재로그인 후 첫 요청이 죽은 토큰의 deadline 로 임박 오판(새 토큰 불필요 reissue).
                // 로그인 요청 자체가 이 인터셉터(MainClient)를 토큰 없이 지나므로 이 분기가 반드시 씻는다.
                expiryTracker.clear()
                // 그냥 바로 다음 인터셉터한테 요청 넘기고 응답 받은 다음에 꺼져라
                return chain.proceed(originalRequest)
            }

            // 만료 임박이면 401 을 기다리지 않고 먼저 갈아끼움 — 실패 시 기존 토큰 유지
            val accessToken =
                if (expiryTracker.isExpiringSoon()) {
                    when (val outcome = tokenReissuer.reissue(expectedAccessToken = storedToken)) {
                        is TokenReissuer.Outcome.TokenAlreadyChanged -> outcome.accessToken
                        is TokenReissuer.Outcome.Rotated -> outcome.accessToken
                        TokenReissuer.Outcome.Failed -> storedToken
                    }
                } else {
                    storedToken
                }

            // 액세스 토큰이 있으면 가기 전에 헤더에 달고 나가라
            val authenticatedRequest =
                originalRequest
                    .newBuilder() // 엄마가 가방 매 줄게
                    .header("Authorization", "Bearer $accessToken") // 가방에 헤더 넣고
                    .build() // 다 맸다

            val response = chain.proceed(authenticatedRequest)
            recordExpiresIn(response, attachedAccessToken = accessToken)
            return response
        }

        /**
         * 성공 응답 봉투에 `expiresIn` 이 있으면 deadline 갱신.
         *
         * 비용 구조에 주의 — peekBody 는 성공 JSON 응답 전체를 1벌 복사하므로 문자열 사전 필터가
         * 아끼는 건 [BaseResponse] 디코드뿐이다. MainClient 가 JSON 봉투 전용이라 수용하는
         * 트레이드오프이고, 미래에 끼어들 수 있는 바이너리/스트리밍 응답은 Content-Type 게이트로
         * 거른다. 크기 상한을 두지 않은 것은 공식 권고("Most applications should set a modest
         * limit on byteCount, such as 1 MiB" — Response.peekBody 문서)와 의도적으로 다른 선택:
         * `expiresIn` 운반 응답이 곧 가장 큰 목록 응답이라 상한이 기능을 조용히 죽일 수 있고,
         * 봉투는 Retrofit 이 어차피 전량 버퍼링하는 JSON 이라 신규 OOM 벡터가 아니기 때문.
         *
         * [attachedAccessToken] 과 현재 저장 토큰이 다르면 기록을 폐기한다 — 응답이 in-flight 인
         * 사이 회전이 끼어들었으면 이 `expiresIn` 은 구 토큰 기준이라, 새 토큰 deadline 을
         * 임박으로 오염시켜 불필요 회전을 유발하기 때문. (401 투명 재시도 응답도 같은 이유로
         * 폐기될 수 있는데, 기록이 비어 있으면 다음 응답에서 다시 기록되는 안전 방향이라 수용.)
         */
        private fun recordExpiresIn(
            response: Response,
            attachedAccessToken: String,
        ) {
            if (!response.isSuccessful) return
            val isJson =
                response.body
                    .contentType()
                    ?.subtype
                    ?.contains("json") == true
            if (!isJson) return
            val rawBody = response.peekBody(Long.MAX_VALUE).string()
            // 대부분의 응답엔 키 자체가 없음 — 봉투 디코드 전에 문자열 검사로 거른다
            if (EXPIRES_IN_KEY_LITERAL !in rawBody) return
            val expiresInSeconds =
                runCatching {
                    json
                        .decodeFromString<BaseResponse<JsonElement>>(rawBody)
                        .expiresIn
                }.getOrNull() ?: return
            // 요청이 서버를 왕복하는 사이 토큰이 교체됐다면(저장 토큰 ≠ 이 요청에 붙였던 토큰),
            // 이 expiresIn 은 서버가 "구 토큰" 기준으로 계산한 수명 — 새 토큰의 deadline 을
            // 몇십 초 남은 것처럼 오염시켜 불필요 reissue 를 유발하므로 버린다.
            // 새 값으로 갱신하지 "못하는" 이유: 이 응답엔 새 토큰의 수명이 안 실려 있다 —
            // 그건 새 토큰으로 나간 다음 요청의 응답이 알려준다 (그때까지 기록 없음 = 안전 방향)
            val currentToken = runBlocking { authRepository.get().getAccessToken() }.getOrNull()
            if (currentToken != attachedAccessToken) return
            expiryTracker.record(expiresInSeconds)
        }

        private companion object {
            /** 사전 필터용 `"expiresIn"` — 키 정본은 [BaseResponse.EXPIRES_IN_SERIAL_NAME]. */
            const val EXPIRES_IN_KEY_LITERAL = "\"" + BaseResponse.EXPIRES_IN_SERIAL_NAME + "\""
        }
    }
