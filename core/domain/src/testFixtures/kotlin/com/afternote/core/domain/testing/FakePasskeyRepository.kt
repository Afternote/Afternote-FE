package com.afternote.core.domain.testing

import com.afternote.core.domain.repository.auth.PasskeyRepository
import com.afternote.core.model.PasskeyAuthenticationOptions
import com.afternote.core.model.Session
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [PasskeyRepository] fake 정본 (#764).
 *
 * [FakeAuthRepository] 와 같은 규약이다 — 기본은 성공을 돌려주고 호출을 기록하며, 특정 실패는
 * `onX` 로 갈아끼운다. 호출 금지 경계가 필요한 테스트는 [strict] 로 시작한다.
 */
class FakePasskeyRepository(
    var options: PasskeyAuthenticationOptions = PasskeyAuthenticationOptions(DEFAULT_REQUEST_JSON),
    var session: Session.DefaultSession = Session.DefaultSession(DEFAULT_ACCESS_TOKEN, DEFAULT_REFRESH_TOKEN),
    var onAuthenticationOptions: (suspend () -> Result<PasskeyAuthenticationOptions>)? = null,
    var onAuthenticate: (suspend (String) -> Result<Session.DefaultSession>)? = null,
) : PasskeyRepository {
    val attemptedAssertions = CopyOnWriteArrayList<String>()

    private val optionsCounter = AtomicInteger()

    val authenticationOptionsCalls: Int get() = optionsCounter.get()
    val authenticateArg: String? get() = attemptedAssertions.lastOrNull()
    val authenticateCallCount: Int get() = attemptedAssertions.size

    override suspend fun authenticationOptions(): Result<PasskeyAuthenticationOptions> {
        optionsCounter.incrementAndGet()
        onAuthenticationOptions?.let { return it() }
        return Result.success(options)
    }

    override suspend fun authenticate(assertionJson: String): Result<Session.DefaultSession> {
        attemptedAssertions += assertionJson
        onAuthenticate?.let { return it(assertionJson) }
        return Result.success(session)
    }

    companion object {
        const val DEFAULT_REQUEST_JSON = """{"challenge":"challenge"}"""
        private const val DEFAULT_ACCESS_TOKEN = "access"
        private const val DEFAULT_REFRESH_TOKEN = "refresh"

        /** 모든 경로를 닫고, 테스트가 쓰는 `onX` 만 명시적으로 연다. */
        fun strict(): FakePasskeyRepository =
            FakePasskeyRepository(
                onAuthenticationOptions = { unexpectedCall("PasskeyRepository.authenticationOptions") },
                onAuthenticate = { unexpectedCall("PasskeyRepository.authenticate") },
            )
    }
}
