package com.afternote.core.domain.usecase.auth

import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakePasskeyRepository
import com.afternote.core.model.PasskeyAuthenticationOptions
import com.afternote.core.model.Session
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * [PasskeyLoginUseCase] 회귀 가드 (#764).
 *
 * 검증 핵심:
 * 1. 옵션 발급은 저장소 결과를 손대지 않고 그대로 넘기는지
 * 2. 검증 성공 시 받은 토큰을 **기존 로그인과 같은** `AuthRepository.saveSession` 으로 저장하는지
 * 3. 검증 실패면 saveSession 을 부르지 않고 그 실패를 그대로 반환(short-circuit)하는지 —
 *    이미 있는 세션이 패스키 실패로 지워지면 안 된다
 * 4. saveSession 실패면 그 실패를 반환하는지
 */
class PasskeyLoginUseCaseTest {
    private fun useCase(
        passkeyRepository: FakePasskeyRepository,
        authRepository: FakeAuthRepository,
    ) = PasskeyLoginUseCase(passkeyRepository = passkeyRepository, authRepository = authRepository)

    @Test
    fun `옵션 발급 - 저장소가 준 요청 원문을 그대로 넘긴다`() {
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticationOptions = { Result.success(PasskeyAuthenticationOptions("""{"challenge":"abc"}""")) }
            }
        val auth = FakeAuthRepository.strict()

        val result = runBlocking { useCase(passkey, auth).requestOptions() }

        assertEquals("""{"challenge":"abc"}""", result.getOrThrow().requestJson)
        assertEquals(0, auth.saveSessionCallCount)
    }

    @Test
    fun `옵션 발급 실패 - 실패를 그대로 돌려준다`() {
        val failure = IOException("offline")
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticationOptions = { Result.failure(failure) }
            }

        val result = runBlocking { useCase(passkey, FakeAuthRepository.strict()).requestOptions() }

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `검증 성공 - assertion 을 그대로 전달하고 발급 토큰으로 saveSession`() {
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticate = { Result.success(Session.DefaultSession(accessToken = "AT", refreshToken = "RT")) }
            }
        val auth =
            FakeAuthRepository.strict().apply {
                onSaveSession = { _, _ -> Result.success(Unit) }
            }

        val result = runBlocking { useCase(passkey, auth)("""{"id":"cid"}""") }

        assertTrue(result.isSuccess)
        assertEquals("""{"id":"cid"}""", passkey.authenticateArg)
        assertEquals("AT" to "RT", auth.saveSessionArgs)
        assertEquals(1, auth.saveSessionCallCount)
    }

    @Test
    fun `검증 실패 - saveSession 을 부르지 않고 실패를 그대로 돌려준다`() {
        val failure = IllegalStateException("passkey verification failed")
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticate = { Result.failure(failure) }
            }
        // strict 는 saveSession 을 error 로 닫아 둔다 — 불리면 이 테스트가 그 자리에서 깨진다.
        val auth = FakeAuthRepository.strict()

        val result = runBlocking { useCase(passkey, auth)("""{"id":"cid"}""") }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(0, auth.saveSessionCallCount)
    }

    @Test
    fun `세션 저장 실패 - 그 실패를 돌려준다`() {
        val failure = IllegalStateException("datastore write failed")
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticate = { Result.success(Session.DefaultSession("AT", "RT")) }
            }
        val auth =
            FakeAuthRepository.strict().apply {
                onSaveSession = { _, _ -> Result.failure(failure) }
            }

        val result = runBlocking { useCase(passkey, auth)("""{"id":"cid"}""") }

        assertSame(failure, result.exceptionOrNull())
    }
}
