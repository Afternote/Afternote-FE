package com.afternote.core.data.repoimpl.auth

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.data.mapper.auth.PasskeyMapper
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.auth.PasskeyRepository
import com.afternote.core.model.PasskeyAuthenticationOptions
import com.afternote.core.model.Session
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.PasskeyApiService
import com.afternote.core.network.token.AccessTokenExpiryTracker
import java.io.IOException
import javax.inject.Inject

internal class PasskeyRepositoryImpl
    @Inject
    constructor(
        private val passkeyApiService: PasskeyApiService,
        // 패스키도 발급 응답에 expiresIn 을 실어 오므로 선제 reissue deadline 을 같은 규칙으로 기록한다 (#408/#410).
        private val expiryTracker: AccessTokenExpiryTracker,
    ) : PasskeyRepository {
        override suspend fun authenticationOptions(): Result<PasskeyAuthenticationOptions> =
            runCatchingCancellable {
                PasskeyMapper.toAuthenticationOptions(passkeyApiService.authenticateOptions().requireData())
            }.mapTransportFailure()

        override suspend fun authenticate(assertionJson: String): Result<Session.DefaultSession> =
            runCatchingCancellable {
                val data =
                    passkeyApiService
                        .authenticate(PasskeyMapper.toAuthenticateRequest(assertionJson))
                        .requireData()
                // AuthRepositoryImpl.recordIssuedExpiresIn 과 같은 규칙 — 서버가 생략하면 이전 토큰 기준
                // stale deadline 이 새 세션에 적용되지 않게 비우고, 만료는 401 안전망이 받는다.
                data.expiresIn?.let(expiryTracker::record) ?: expiryTracker.clear()
                AuthMapper.toDefaultLoginResult(data)
            }.mapTransportFailure()
    }

/**
 * 전송 계층 실패만 도메인 예외로 옮긴다.
 *
 * `AuthRepositoryImpl.mapLoginFailure` 와 달리 서버 `code` 는 가르지 않는다. 패스키 실패 코드
 * 4종(BE `ErrorCode` 2700~2703 — challenge 만료·검증 실패·미등록 자격·중복 등록)은 어느 것도
 * 사용자가 화면에서 고칠 수 있는 갈래가 아니라, 지금은 전부 같은 안내 하나로 내려앉는다.
 * 코드별로 다른 안내가 필요해지면 그때 가른다 — 미리 갈라 두면 쓰이지 않는 분기만 남는다.
 *
 * 반면 [IOException] 은 갈라야 한다. 서버가 거절한 것과 요청이 서버에 닿지도 못한 것은
 * 소비처의 대응이 다르고, presentation 은 `core:network` 를 보지 않아 이 치환 없이는 구분할 수 없다.
 */
private fun <T> Result<T>.mapTransportFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is IOException -> Result.failure(CoreAuthFailure.NetworkUnavailable(exception))
        else -> this
    }
