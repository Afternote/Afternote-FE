package com.afternote.core.data.repoimpl.auth

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.datastore.LocalStoreRegistry
import com.afternote.core.datastore.StoreScope
import com.afternote.core.datastore.TokenDataSource
import com.afternote.core.domain.error.InvalidLoginCredentialsException
import com.afternote.core.domain.error.NetworkUnavailableException
import com.afternote.core.domain.error.SocialLoginRejectedException
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.network.dto.LoginRequestDto
import com.afternote.core.network.dto.LogoutRequestDto
import com.afternote.core.network.dto.ReissueRequestDto
import com.afternote.core.network.dto.SocialLoginRequestDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.AuthApiService
import com.afternote.core.network.service.TokenApiService
import com.afternote.core.network.token.AccessTokenExpiryTracker
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject

internal class AuthRepositoryImpl
    @Inject
    constructor(
        private val tokenDataSource: TokenDataSource,
        private val authApiService: AuthApiService,
        private val tokenApiService: TokenApiService,
        // 발급(로그인) 응답의 expiresIn 으로 선제 reissue deadline 을 기록하고, 세션 종료 시 함께 정리한다 (#408/#410).
        private val expiryTracker: AccessTokenExpiryTracker,
        // 로그아웃·탈퇴 시 SESSION 스코프 로컬 저장소 일괄 정리 (#912).
        private val localStoreRegistry: LocalStoreRegistry,
    ) : AuthRepository {
        override suspend fun clearSession() =
            runCatchingCancellable {
                localStoreRegistry.clearScope(StoreScope.SESSION)
                // tracker 는 network 계층 in-memory 상태라 레지스트리(디스크 저장소 전담) 관할 밖.
                // 남기면 재로그인 후 이전 토큰 기준 deadline 으로 만료 임박을 오판한다.
                expiryTracker.clear()
            }

        override suspend fun getAccessToken() = runCatchingCancellable { tokenDataSource.getAccessToken() }

        override suspend fun getRefreshToken() = runCatchingCancellable { tokenDataSource.getRefreshToken() }

        override suspend fun saveSession(
            accessToken: String,
            refreshToken: String,
        ) = runCatchingCancellable {
            tokenDataSource.saveTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }

        override val isLoggedIn: Flow<Boolean>
            get() = tokenDataSource.isLoggedIn

        override suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ) = runCatchingCancellable {
            tokenDataSource.updateTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }

        // TODO:레거시 레포에 있던 authApiService 관련이고 리팩토링해야 하는지 검사 필요

        override suspend fun defaultLogin(
            email: String,
            password: String,
        ): Result<Session.DefaultSession> =
            runCatchingCancellable {
                val data = authApiService.login(LoginRequestDto(email, password)).requireData()
                recordIssuedExpiresIn(data.expiresIn)
                AuthMapper.toDefaultLoginResult(data)
            }.mapLoginFailure()

        override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> =
            runCatchingCancellable {
                val data =
                    authApiService
                        .socialLogin(
                            SocialLoginRequestDto(
                                provider = "KAKAO",
                                accessToken = oauthToken,
                            ),
                        ).requireData()
                recordIssuedExpiresIn(data.expiresIn)
                AuthMapper.toSocialLoginResult(data)
            }.mapLoginFailure()

        override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> =
            runCatchingCancellable {
                val data =
                    authApiService
                        .socialLogin(
                            SocialLoginRequestDto(
                                provider = "GOOGLE",
                                accessToken = idToken,
                            ),
                        ).requireData()
                recordIssuedExpiresIn(data.expiresIn)
                AuthMapper.toSocialLoginResult(data)
            }.mapLoginFailure()

        override suspend fun rotateToken(): Result<TokenBundle> =
            runCatchingCancellable {
                val refreshToken =
                    getRefreshToken().getOrNull()
                        ?: error("리프레시 토큰이 존재하지 않습니다.")
                val response = tokenApiService.reissue(ReissueRequestDto(refreshToken))
                val tokenBundleResult = AuthMapper.toRotateTokenResult(response.requireData())
                check(tokenBundleResult.accessToken.isNotEmpty()) {
                    "Token rotation returned an empty access token"
                }
                updateTokens(
                    accessToken = tokenBundleResult.accessToken,
                    refreshToken = tokenBundleResult.refreshToken,
                ).getOrThrow()
                tokenBundleResult
            }

        /**
         * 서버 로그아웃은 best-effort (네트워크 실패해도 사용자는 로그아웃 상태로 가야 함).
         * SESSION 스코프 로컬 저장소와 선제 reissue deadline 은 서버 호출 결과와 무관하게 항상 정리한다.
         */
        override suspend fun logout(): Result<Unit> =
            runCatchingCancellable {
                val refreshToken = getRefreshToken().getOrNull()
                if (refreshToken != null) {
                    runCatchingCancellable { authApiService.logout(LogoutRequestDto(refreshToken)) }
                }
                // 두 정리 모두 반드시 위 API 호출 뒤여야 한다. 로그아웃 HTTP 요청도 AuthInterceptor 를
                // 지나므로 그 시점엔 토큰이 살아 있어야 하고, 토큰이 만료 임박이면 요청 직전 선제 reissue 가
                // tracker 에 새 deadline 을 기록할 수 있다 — tracker 를 먼저 비우면 그 기록이 되살아난다.
                localStoreRegistry.clearScope(StoreScope.SESSION)
                expiryTracker.clear()
            }

        /**
         * 발급 응답의 [expiresInSeconds](잔여 수명 초)로 선제 reissue deadline 을 기록한다.
         * 서버가 생략하면(null) 기존 deadline 을 비운다 — 남기면 이전 토큰 기준 stale deadline 이
         * 새 세션에 적용될 수 있어서다. `TokenReissuer` 회전 경로와 같은 규칙: 다음 발급 응답이
         * 채울 때까지 선제 갱신을 쉬고, 만료는 401 안전망(`TokenAuthenticator`)이 받는다.
         */
        private fun recordIssuedExpiresIn(expiresInSeconds: Long?) {
            expiresInSeconds?.let(expiryTracker::record) ?: expiryTracker.clear()
        }
    }

// BE `ErrorCode.java` 대조 — `AuthService.login()`·`socialLogin()` 이 실제로 던지는 거절만 담았다.
// 대역 판정(4xx 통과)으로 바꾸지 말 것: [ApiException.code] 는 HTTP 상태가 아니라 본문의
// 비즈니스 코드고, BE 에는 5xx 에 붙는 코드도 있다(1904).
private const val CODE_USER_NOT_FOUND = 1201
private const val CODE_PASSWORD_MISMATCH = 1202
private const val CODE_SOCIAL_LOGIN_FAILED = 1208
private const val CODE_UNSUPPORTED_SOCIAL_LOGIN = 1209

/**
 * 로그인 실패를 도메인 예외로 옮긴다 — 가르는 신호는 서버 봉투의 `code` 뿐이고 `message` 는
 * 옮기지 않는다(BE#92 — 사용자 노출용이라는 규정이 없어 계약이 아니다). 사유가 확인된 실패만
 * 치환하고 나머지는 그대로 두어, 소비처가 일반 문구로 내려앉는다(5xx 본문 실측 #511).
 *
 * [ApiException] 을 먼저 거르는 이유 — IOException 서브클래스라 순서를 바꾸면 서버 응답 실패가
 * 전송 실패로 잡힌다. 취소는 다시 보지 않는다 — 호출부가 전부 [runCatchingCancellable] 이라
 * `CancellationException` 이 [Result] 에 담긴 채로 도달하지 않는다.
 */
private fun <T> Result<T>.mapLoginFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is ApiException -> {
            when (exception.code) {
                CODE_USER_NOT_FOUND, CODE_PASSWORD_MISMATCH -> {
                    Result.failure(InvalidLoginCredentialsException(exception))
                }

                CODE_SOCIAL_LOGIN_FAILED, CODE_UNSUPPORTED_SOCIAL_LOGIN -> {
                    Result.failure(SocialLoginRejectedException(exception))
                }

                else -> {
                    this
                }
            }
        }

        is IOException -> {
            Result.failure(NetworkUnavailableException(exception))
        }

        else -> {
            this // 성공·그 외 예외 모두 통과
        }
    }
