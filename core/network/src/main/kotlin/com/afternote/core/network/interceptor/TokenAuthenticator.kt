package com.afternote.core.network.interceptor

import android.util.Log
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.network.token.TokenReissuer
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator
    @Inject
    constructor(
        private val authRepository: dagger.Lazy<AuthRepository>,
        private val tokenReissuer: TokenReissuer,
    ) : Authenticator {
        @Suppress("ReturnCount")
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (response.responseCount >= 3) {
                Log.e("TokenAuthenticator", "❌ 무한 루프 방지: 재시도 횟수 3회 이상. 세션 만료 처리")
                runBlocking { authRepository.get().clearSession() }
                return null
            }

            val originalRequest = response.request
            val authHeader = originalRequest.header("Authorization")
            val oldAccessToken =
                authHeader?.let {
                    if (it.startsWith("Bearer ", ignoreCase = true)) it.substring(7) else it
                }
            if (oldAccessToken == null) {
                Log.e("TokenAuthenticator", "❌ 인증 실패: 직전 요청이 애초에 토큰을 포함하지 않았음")
                return null
            }

            // 회전은 선제 갱신 경로(AuthInterceptor)와 공유하는 단일 락(TokenReissuer) 경유 (#408) —
            // 앞선 다른 경로/스레드가 이미 회전했으면 TokenAlreadyChanged 로 새 토큰만 받아 재시도
            return when (val outcome = tokenReissuer.reissue(expectedAccessToken = oldAccessToken)) {
                is TokenReissuer.Outcome.TokenAlreadyChanged -> {
                    originalRequest.withBearer(outcome.accessToken)
                }

                is TokenReissuer.Outcome.Rotated -> {
                    if (outcome.accessToken == oldAccessToken) {
                        Log.e("TokenAuthenticator", "❌ 리이슈 실패: 서버가 이전과 동일한 토큰을 반환함")
                        runBlocking { authRepository.get().clearSession() }
                        null
                    } else {
                        originalRequest.withBearer(outcome.accessToken)
                    }
                }

                TokenReissuer.Outcome.Failed -> {
                    Log.e("TokenAuthenticator", "❌ 리이슈 실패: 세션 만료. 로그아웃 처리 진행")
                    runBlocking { authRepository.get().clearSession() }
                    null
                }
            }
        }
    }

/** 액세스 토큰만 갈아 끼운 재시도용 요청 사본 (OkHttp 공식 recipes 의 인증 예제 형태). */
private fun Request.withBearer(accessToken: String) =
    newBuilder()
        .header("Authorization", "Bearer $accessToken")
        .build()

/**
 * 이 응답까지의 시도 횟수 — OkHttp 는 재시도마다 직전 시도의 응답을 [Response.priorResponse]
 * 로 매달아 사슬을 만든다. generateSequence 는 현재 응답에서 시작해 priorResponse 가 null
 * (첫 시도)이 될 때까지 사슬을 따라가고, count 가 그 길이 = 총 시도 횟수다.
 * (develop 의 while 루프와 동일 계산 — OkHttp 공식 recipes 의 Kotlin 형태.)
 */
private val Response.responseCount: Int
    get() = generateSequence(this) { it.priorResponse }.count()
