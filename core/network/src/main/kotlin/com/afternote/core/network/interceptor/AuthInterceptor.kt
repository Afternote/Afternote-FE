package com.afternote.core.network.interceptor

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.network.token.AccessTokenExpiryTracker
import com.afternote.core.network.token.TokenReissuer
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * 액세스 토큰을 요청 헤더에 부착하고, 토큰 수명을 관리하는 인터셉터.
 *
 * 부착 외에 선제 토큰 갱신(#408)을 함께 한다 — "유효한 토큰을 부착한다"는 같은 관심사다:
 * 요청 전 [AccessTokenExpiryTracker] 기준 잔여 수명이 임계값 미만이면 선제 reissue 한다.
 * 회전은 [TokenReissuer] 단일 락 경유라 401 경로(`TokenAuthenticator`)와 이중 실행되지
 * 않는다. 실패해도 기존 토큰으로 진행 — 401 사후 대응이 안전망이고, 선제 경로는 clearSession
 * 하지 않는다(일시 오류로 세션을 날리면 안 됨). reissue 요청 자체는 토큰 미부착 `RefreshClient`
 * 의 별도 Retrofit 을 타므로 재귀 없음.
 *
 * deadline 의 입력값 `expiresIn` 은 BE #410(2026-06-20)으로 발급 응답(로그인·reissue)의 `data`
 * 에 실려 와, 토큰을 발급/회전하는 곳(`AuthRepositoryImpl`·[TokenReissuer])이 직접 기록한다 —
 * 과거 모든 성공 응답 본문을 peek 해 봉투 `expiresIn` 을 수집하던 횡단 처리는 제거됐다(#410).
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

            return chain.proceed(authenticatedRequest)
        }
    }
