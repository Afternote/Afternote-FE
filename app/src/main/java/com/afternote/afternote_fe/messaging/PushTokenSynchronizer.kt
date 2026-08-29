package com.afternote.afternote_fe.messaging

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.push.DevicePushTokenProvider
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.repository.push.PushTokenRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이 기기의 FCM 식별자(FID)를 서버에 등록해 둔다 (#1493).
 *
 * 등록 시점은 두 곳이다.
 * 1. **로그인이 확정될 때마다** — 최초 로그인뿐 아니라 이미 로그인된 채 앱을 켠 경우도 포함한다.
 *    재설치·앱 데이터 삭제로 식별자가 새로 발급됐거나 서버 쪽 기록이 사라졌을 수 있어, 서버가
 *    upsert 로 받아 주는 것을 믿고 실행마다 한 번 다시 알린다.
 * 2. **`onRegistered` 로 FID 가 회전할 때** — 로그인 상태가 아니면 보류한다. 다음 로그인 확정이
 *    1번 경로로 새 식별자를 싣는다.
 *
 * 두 경로는 거의 동시에 발화한다 — 1번이 부르는 `register()` 가 성공하면 그 자리에서 2번 콜백이
 * 뜨기 때문이다. 같은 값을 두 번 보내면 서버가 동시 upsert 를 처리하다 500 을 돌려주므로
 * (0829 실측), 뮤텍스로 직렬화하고 직전에 성공한 값은 다시 보내지 않는다.
 *
 * 해제(로그아웃)는 여기가 아니라 `AuthRepositoryImpl.logout()` 이 맡는다 — 세션이 아직 살아 있는
 * 그 시점에만 서버 호출이 통하기 때문이다. 여기서는 로그아웃을 보면 캐시만 비워, 다음 로그인이
 * 다시 등록하도록 한다.
 */
@Singleton
class PushTokenSynchronizer
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val devicePushTokenProvider: DevicePushTokenProvider,
        private val pushTokenRepository: PushTokenRepository,
        private val errorReporter: ErrorReporter,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var lastRegistered: String? = null

        /** 로그인 상태를 계속 지켜본다. 앱 프로세스가 사는 동안 돈다. */
        suspend fun observeLogin() {
            authRepository.isLoggedIn
                .distinctUntilChanged()
                .collect { isLoggedIn ->
                    if (isLoggedIn) {
                        registerCurrentToken()
                    } else {
                        mutex.withLock { lastRegistered = null }
                    }
                }
        }

        /** FID 회전 통보를 받았을 때. 로그인 상태가 아니면 아무것도 하지 않는다. */
        suspend fun onTokenRotated(token: String) {
            if (!isLoggedIn()) return
            registerOnce(token)
        }

        private suspend fun registerCurrentToken() {
            val token = devicePushTokenProvider.currentToken() ?: return
            registerOnce(token)
        }

        /** 같은 값을 연달아 보내지 않는다 — 서버 동시 upsert 가 500 을 내기 때문이다. */
        private suspend fun registerOnce(token: String) {
            mutex.withLock {
                if (lastRegistered == token) return@withLock
                pushTokenRepository
                    .register(token)
                    .onSuccess { lastRegistered = token }
                    .onFailure { error ->
                        errorReporter.recordFailure(error, mapOf("stage" to "push_token_register"))
                    }
            }
        }

        private suspend fun isLoggedIn(): Boolean =
            runCatchingCancellable {
                authRepository.getAccessToken().getOrNull() != null
            }.getOrNull() ?: false
    }
