package com.afternote.core.network.token

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.network.model.ApiException
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 선제 갱신과 401 대응이 공유하는 토큰 재발급 single-flight.
 *
 * 락 안에서 호출자가 본 토큰과 현재 저장 토큰을 다시 비교해 다른 경로가 이미 갱신했다면
 * 중복 재발급을 건너뛴다.
 *
 * 락은 회전을 직렬화할 뿐 **실패 결과를 공유하지 않아** 구멍이 있었다(#1126). 회전이 성공하면
 * 저장 토큰이 바뀌어 대기자가 [Outcome.TokenAlreadyChanged] 로 빠지지만, 실패하면 토큰이
 * 그대로라 대기자가 각자 다시 재발급 HTTP 를 쳤다. 그래서 확정 거절([Outcome.AuthenticationRejected])
 * 에 대해서는 두 가지를 여기서 닫는다.
 *
 *  - **세션 정리를 락 안에서 끝낸다.** 정리가 호출자 쪽(락 밖)에 있으면 락이 풀리고 정리가
 *    끝나기까지가 대기자의 재발급이 빠져나가는 창이다(실측 3ms, 재발급 HTTP 2회).
 *  - **거절 결과를 그 액세스 토큰에 묶어 공유한다.** 같은 토큰으로 들어온 대기자는 재발급을
 *    치지 않고 같은 [Outcome.AuthenticationRejected] 를 받아, 한 번의 실패가 하나의 분류로
 *    확정된다. 일시 실패는 공유하지 않는다 — 재시도가 성립하는 실패라 캐시하면 복구를 막는다.
 */
@Singleton
class TokenReissuer
    @Inject
    constructor(
        private val authRepository: dagger.Lazy<AuthRepository>,
        private val expiryTracker: AccessTokenExpiryTracker,
        private val errorReporter: ErrorReporter,
    ) {
        sealed interface Outcome {
            /** 다른 경로가 먼저 토큰을 갱신해 재발급을 생략함. */
            data class TokenAlreadyChanged(
                val accessToken: String,
            ) : Outcome

            /** 현재 호출이 새 토큰 발급을 완료함. */
            data class Rotated(
                val accessToken: String,
            ) : Outcome

            sealed interface Failure : Outcome {
                val exception: Throwable
            }

            data class AuthenticationRejected(
                override val exception: Throwable,
            ) : Failure

            data class TransportFailure(
                override val exception: IOException,
            ) : Failure

            data class ServerFailure(
                override val exception: Throwable,
            ) : Failure

            data class UnexpectedFailure(
                override val exception: Throwable,
            ) : Failure
        }

        /**
         * 확정 거절이 난 액세스 토큰과 그 결과 (#1126).
         *
         * 세션 정리까지 끝난 뒤라 저장 토큰은 비어 있고, 그 상태로 [AuthRepository.rotateToken] 을
         * 다시 부르면 refresh 부재로 `IllegalStateException` → [Outcome.UnexpectedFailure] =
         * "세션 유지" 가 되어 같은 실패가 두 분류로 갈라진다. 액세스 토큰을 키로 삼는 이유는
         * 재로그인이 새 토큰을 주므로 키가 저절로 어긋나 캐시가 만료되기 때문이다 — 시계가 필요 없다.
         */
        private var rejectedAccessToken: String? = null
        private var rejection: Outcome.AuthenticationRejected? = null

        /** @param expectedAccessToken 호출자가 교체하려는 기존 액세스 토큰. */
        fun reissue(expectedAccessToken: String): Outcome {
            synchronized(this) {
                // 저장 토큰 비교보다 먼저다 — 세션 정리 뒤엔 저장 토큰이 비어 있어 아래 가드를 그냥 통과한다.
                rejection?.let { if (expectedAccessToken == rejectedAccessToken) return it }

                val currentToken = runBlocking { authRepository.get().getAccessToken() }.getOrNull()
                if (!currentToken.isNullOrEmpty() && currentToken != expectedAccessToken) {
                    return Outcome.TokenAlreadyChanged(currentToken)
                }

                val rotationResult = runBlocking { authRepository.get().rotateToken() }
                val rotationException = rotationResult.exceptionOrNull()
                if (rotationException != null) {
                    // 실패한 토큰의 deadline 을 지워 선제 재시도 반복을 막는다.
                    expiryTracker.clear()
                    val failure = classifyFailure(rotationException)
                    rememberIfRejected(expectedAccessToken, failure)
                    reportObservableFailure(failure)
                    return failure
                }

                val newBundle = rotationResult.getOrThrow()
                if (newBundle.accessToken.isEmpty()) {
                    expiryTracker.clear()
                    val failure =
                        Outcome.UnexpectedFailure(
                            IllegalStateException("Token rotation returned an empty access token"),
                        )
                    reportObservableFailure(failure)
                    return failure
                }

                // 만료 정보가 없으면 이전 토큰의 deadline 을 남기지 않는다.
                newBundle.expiresIn?.let(expiryTracker::record) ?: expiryTracker.clear()
                rejectedAccessToken = null
                rejection = null
                return Outcome.Rotated(newBundle.accessToken)
            }
        }

        private fun classifyFailure(exception: Throwable): Outcome.Failure =
            when (exception) {
                is ApiException -> classifyApiFailure(exception)
                is HttpException -> classifyHttpFailure(exception, exception.code())
                is IOException -> Outcome.TransportFailure(exception)
                else -> Outcome.UnexpectedFailure(exception)
            }

        private fun classifyApiFailure(exception: ApiException): Outcome.Failure =
            if (exception.code == CODE_INVALID_REFRESH_TOKEN) {
                Outcome.AuthenticationRejected(exception)
            } else {
                classifyHttpFailure(exception, exception.status)
            }

        /**
         * 재발급 엔드포인트 전용 분류다 — 일반 API 응답에 쓰면 안 된다.
         *
         * 400 이 거절인 이유(#1126): 무효 refresh 의 `code=1107` 은 **본문 파싱에 성공해야** 읽힌다.
         * 파싱이 실패하면 400 은 401 도 403 도 5xx 도 아니라 `else` = [Outcome.UnexpectedFailure] =
         * "세션 유지" 로 떨어졌고, 그러면 무효 refresh 가 세션에 남아 이후 요청이 401 → 재발급 400 을
         * 반복한다(사용자는 로그인 화면으로도 못 가고 데이터도 못 받는다). 이 엔드포인트에 한해
         * 400 은 "이 refresh 로는 더 진행할 수 없다" 이므로 본문과 무관하게 거절로 확정한다.
         */
        private fun classifyHttpFailure(
            exception: Throwable,
            status: Int,
        ): Outcome.Failure =
            when (status) {
                400, 401, 403 -> Outcome.AuthenticationRejected(exception)
                in 500..599 -> Outcome.ServerFailure(exception)
                else -> Outcome.UnexpectedFailure(exception)
            }

        /**
         * 확정 거절이면 락 안에서 세션을 정리하고 결과를 보존한다 (#1126).
         *
         * 정리를 락 밖(호출자)에 두면 락이 풀린 뒤 정리가 끝나기까지가 대기자의 재발급이
         * 빠져나가는 창이다. 일시 실패는 여기 들어오지 않는다 — 세션도 유지하고 캐시도 하지 않는다.
         */
        private fun rememberIfRejected(
            expectedAccessToken: String,
            failure: Outcome.Failure,
        ) {
            if (failure !is Outcome.AuthenticationRejected) return
            runBlocking { authRepository.get().clearSession() }
            rejectedAccessToken = expectedAccessToken
            rejection = failure
        }

        private fun reportObservableFailure(failure: Outcome.Failure) {
            val failureKind =
                when (failure) {
                    is Outcome.AuthenticationRejected -> return
                    is Outcome.TransportFailure -> "transport"
                    is Outcome.ServerFailure -> "server"
                    is Outcome.UnexpectedFailure -> "unexpected"
                }
            errorReporter.recordFailure(
                throwable = failure.exception,
                attributes =
                    mapOf(
                        "auth_stage" to "token_reissue",
                        "failure_kind" to failureKind,
                    ),
            )
        }
    }

private const val CODE_INVALID_REFRESH_TOKEN = 1107
