package com.afternote.core.network.interceptor

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.network.token.TokenReissuer
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject

class TokenAuthenticator
    @Inject
    constructor(
        private val authRepository: dagger.Lazy<AuthRepository>,
        private val tokenReissuer: TokenReissuer,
        private val errorReporter: ErrorReporter,
    ) : Authenticator {
        @Suppress("ReturnCount")
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            if (response.responseCount >= 3) {
                errorReporter.recordAuthContractViolation(AUTH_STAGE_RETRY_LIMIT)
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
                errorReporter.recordAuthContractViolation(AUTH_STAGE_MISSING_AUTH_HEADER)
                return null
            }

            // 회전은 선제 갱신 경로(AuthInterceptor)와 공유하는 단일 락(TokenReissuer) 경유 (#408) —
            // 앞선 다른 경로/스레드가 이미 회전했으면 TokenAlreadyChanged 로 새 토큰만 받아 재시도.
            // 인증 거절의 세션 정리는 락 안에서 끝난다 (#1126) — 여기서 정리하면 락이 풀린 뒤
            // 정리가 끝나기까지가 대기자의 중복 재발급이 빠져나가는 창이 된다.
            return when (val outcome = tokenReissuer.reissue(expectedAccessToken = oldAccessToken)) {
                is TokenReissuer.Outcome.TokenAlreadyChanged -> {
                    originalRequest.withBearer(outcome.accessToken)
                }

                is TokenReissuer.Outcome.Rotated -> {
                    if (outcome.accessToken == oldAccessToken) {
                        errorReporter.recordAuthContractViolation(AUTH_STAGE_SAME_TOKEN)
                        runBlocking { authRepository.get().clearSession() }
                        null
                    } else {
                        originalRequest.withBearer(outcome.accessToken)
                    }
                }

                is TokenReissuer.Outcome.AuthenticationRejected -> {
                    null
                }

                is TokenReissuer.Outcome.Failure -> {
                    throw TokenReissueFailureException(outcome.exception)
                }
            }
        }
    }

private fun ErrorReporter.recordAuthContractViolation(authStage: String) {
    recordFailure(
        throwable = IllegalStateException("Token authenticator contract violation"),
        attributes = mapOf(KEY_AUTH_STAGE to authStage),
    )
}

/** 재발급의 기술 원문을 UI 에 노출하지 않고 현재 요청만 실패시키는 예외. */
private class TokenReissueFailureException(
    cause: Throwable,
) : IOException(null, cause)

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

private const val KEY_AUTH_STAGE = "auth_stage"
private const val AUTH_STAGE_RETRY_LIMIT = "retry_limit"
private const val AUTH_STAGE_MISSING_AUTH_HEADER = "missing_auth_header"
private const val AUTH_STAGE_SAME_TOKEN = "same_token"
