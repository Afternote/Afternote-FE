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
 * 중복 재발급을 건너뛴다. 세션 정리 여부는 실패 유형을 받은 호출자가 결정한다.
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

        /** @param expectedAccessToken 호출자가 교체하려는 기존 액세스 토큰. */
        fun reissue(expectedAccessToken: String): Outcome {
            synchronized(this) {
                val currentToken = runBlocking { authRepository.get().getAccessToken() }.getOrNull()
                if (!currentToken.isNullOrEmpty() && currentToken != expectedAccessToken) {
                    return Outcome.TokenAlreadyChanged(currentToken)
                }

                val rotationResult = runBlocking { authRepository.get().rotateToken() }
                val newBundle = rotationResult.getOrNull()
                val newAccessToken = newBundle?.accessToken

                return if (newAccessToken.isNullOrEmpty()) {
                    // 실패한 토큰의 deadline 을 지워 선제 재시도 반복을 막는다.
                    expiryTracker.clear()
                    Outcome.Failed
                } else {
                    // 만료 정보가 없으면 이전 토큰의 deadline 을 남기지 않는다.
                    newBundle.expiresIn?.let(expiryTracker::record) ?: expiryTracker.clear()
                    Outcome.Rotated(newAccessToken)
                }
            }
        }

        private fun classifyFailure(exception: Throwable): Outcome.Failure =
            when (exception) {
                is ApiException -> classifyHttpFailure(exception, exception.status)
                is HttpException -> classifyHttpFailure(exception, exception.code())
                is IOException -> Outcome.TransportFailure(exception)
                else -> Outcome.UnexpectedFailure(exception)
            }

        private fun classifyHttpFailure(
            exception: Throwable,
            status: Int,
        ): Outcome.Failure =
            when (status) {
                401, 403 -> Outcome.AuthenticationRejected(exception)
                in 500..599 -> Outcome.ServerFailure(exception)
                else -> Outcome.UnexpectedFailure(exception)
            }

        private fun reportObservableFailure(failure: Outcome.Failure) {
            if (failure !is Outcome.TransportFailure && failure !is Outcome.ServerFailure) return
            errorReporter.recordFailure(
                throwable = failure.exception,
                attributes = mapOf("auth_stage" to "token_reissue"),
            )
        }
    }
