package com.afternote.afternote_fe.messaging

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.push.DevicePushTokenProvider
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.repository.push.PushTokenRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이 기기의 FCM 토큰을 서버에 등록해 둔다 (#1493).
 *
 * 등록 시점은 두 곳이다.
 * 1. **로그인이 확정될 때마다** — 최초 로그인뿐 아니라 이미 로그인된 채 앱을 켠 경우도 포함한다.
 *    재설치·앱 데이터 삭제로 토큰이 새로 발급됐거나 서버 쪽 기록이 사라졌을 수 있어, 서버가
 *    upsert 로 받아 주는 것을 믿고 실행마다 한 번 다시 알린다.
 * 2. **`onNewToken` 으로 토큰이 회전할 때** — 로그인 상태가 아니면 보류한다. 다음 로그인 확정이
 *    1번 경로로 새 토큰을 싣는다.
 *
 * 해제(로그아웃)는 여기가 아니라 `AuthRepositoryImpl.logout()` 이 맡는다 — 세션이 아직 살아 있는
 * 그 시점에만 서버 호출이 통하기 때문이다.
 */
@Singleton
class PushTokenSynchronizer
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val devicePushTokenProvider: DevicePushTokenProvider,
        private val pushTokenRepository: PushTokenRepository,
    ) {
        /** 로그인 확정을 계속 지켜보며 등록한다. 앱 프로세스가 사는 동안 돈다. */
        suspend fun observeLogin() {
            authRepository.isLoggedIn
                .distinctUntilChanged()
                .filter { isLoggedIn -> isLoggedIn }
                .collect { registerCurrentToken() }
        }

        /** 토큰 회전 통보를 받았을 때. 로그인 상태가 아니면 아무것도 하지 않는다. */
        suspend fun onTokenRotated(token: String) {
            if (!isLoggedIn()) return
            pushTokenRepository.register(token)
        }

        private suspend fun registerCurrentToken() {
            val token = devicePushTokenProvider.currentToken() ?: return
            pushTokenRepository.register(token)
        }

        private suspend fun isLoggedIn(): Boolean =
            runCatchingCancellable {
                authRepository.getAccessToken().getOrNull() != null
            }.getOrNull() ?: false
    }
