package com.afternote.core.data.repoimpl.auth

import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.PasskeyAuthenticateRequestDto
import com.afternote.core.network.dto.PasskeyAuthenticationOptionsDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.PasskeyApiService
import com.afternote.core.network.token.AccessTokenExpiryTracker
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

/**
 * [PasskeyRepositoryImpl] 회귀 가드 (#764).
 *
 * 계약 —
 * 1. 옵션 응답을 Credential Manager 가 먹는 요청 원문으로 옮긴다
 * 2. 검증 성공 응답은 **기존 로그인과 같은** 토큰 봉투이므로 `expiresIn` 을 같은 규칙으로
 *    deadline 에 기록하고, 생략되면 비운다(stale 방지 — `AuthRepositoryImpl` 과 동일)
 * 3. 전송 계층 실패(IO)만 [CoreAuthFailure.NetworkUnavailable] 로 옮기고, 서버가 응답한
 *    실패([ApiException])는 치환하지 않아 소비처가 일반 안내로 내려앉는다
 *
 * [AccessTokenExpiryTracker] 는 실물 사용 — 시계는 `isReturnDefaultValues` 로 0 에 고정되므로
 * 잔여 30초 기록 = 임박 true / 비움 = false 로 상태를 관찰한다(임계 60초).
 */
class PasskeyRepositoryImplTest {
    private val tracker = AccessTokenExpiryTracker()

    private fun repository(apiService: PasskeyApiService) = PasskeyRepositoryImpl(passkeyApiService = apiService, expiryTracker = tracker)

    @Test
    fun `authenticationOptions - 서버 옵션을 요청 원문으로 옮긴다`() {
        val result = runBlocking { repository(FakePasskeyApiService()).authenticationOptions() }

        assertTrue(result.getOrThrow().requestJson.contains("\"challenge\":\"chal\""))
    }

    @Test
    fun `authenticationOptions - 전송 실패는 NetworkUnavailable 로 옮긴다`() {
        val apiService = FakePasskeyApiService(onAuthenticateOptions = { throw UnknownHostException("no dns") })

        val result = runBlocking { repository(apiService).authenticationOptions() }

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.NetworkUnavailable)
    }

    @Test
    fun `authenticate - assertion 을 credential 로 감싸 보내고 세션을 돌려준다`() {
        val apiService = FakePasskeyApiService()

        val session = runBlocking { repository(apiService).authenticate("""{"id":"cid"}""") }.getOrThrow()

        assertEquals("AT", session.accessToken)
        assertEquals("RT", session.refreshToken)
        assertEquals(
            JsonPrimitive("cid"),
            apiService.sentBody
                ?.credential
                ?.jsonObject
                ?.get("id"),
        )
    }

    @Test
    fun `authenticate - 발급 응답 expiresIn 을 deadline 으로 기록`() {
        val apiService = FakePasskeyApiService(expiresIn = 30L)

        runBlocking { repository(apiService).authenticate("""{"id":"cid"}""") }

        assertTrue(tracker.isExpiringSoon())
    }

    @Test
    fun `authenticate - expiresIn 생략이면 옛 deadline 을 비운다`() {
        tracker.record(30L)
        val apiService = FakePasskeyApiService(expiresIn = null)

        runBlocking { repository(apiService).authenticate("""{"id":"cid"}""") }

        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `authenticate - 서버가 응답한 실패는 치환하지 않는다`() {
        // 패스키 실패 코드(BE 2700~2703)는 화면에서 고칠 갈래가 없어 하나의 안내로 모은다 —
        // 여기서 도메인 타입으로 옮기면 쓰이지 않는 분기만 늘어난다.
        val apiException = ApiException(status = 401, code = 2701, serverMessage = null, fallbackMessage = "f")
        val apiService = FakePasskeyApiService(onAuthenticate = { throw apiException })

        val result = runBlocking { repository(apiService).authenticate("""{"id":"cid"}""") }

        assertSame(apiException, result.exceptionOrNull())
    }

    @Test
    fun `authenticate - 전송 실패는 NetworkUnavailable 로 옮긴다`() {
        val apiService = FakePasskeyApiService(onAuthenticate = { throw UnknownHostException("no dns") })

        val result = runBlocking { repository(apiService).authenticate("""{"id":"cid"}""") }

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.NetworkUnavailable)
    }
}

/** [PasskeyApiService] 테스트 공용 가짜 — 보낸 본문을 기록하고, 실패는 `onX` 로 갈아끼운다. */
private class FakePasskeyApiService(
    private val expiresIn: Long? = 3600L,
    private val onAuthenticateOptions: (() -> BaseResponse<PasskeyAuthenticationOptionsDto>)? = null,
    private val onAuthenticate: (() -> BaseResponse<LoginDto.DefaultLoginDto>)? = null,
) : PasskeyApiService {
    var sentBody: PasskeyAuthenticateRequestDto? = null
        private set

    override suspend fun authenticateOptions(): BaseResponse<PasskeyAuthenticationOptionsDto> {
        onAuthenticateOptions?.let { return it() }
        return BaseResponse(
            status = 200,
            code = 200,
            data =
                PasskeyAuthenticationOptionsDto(
                    challenge = "chal",
                    timeout = 300_000L,
                    rpId = "afternote.kro.kr",
                    allowCredentials = emptyList(),
                    userVerification = "required",
                ),
        )
    }

    override suspend fun authenticate(body: PasskeyAuthenticateRequestDto): BaseResponse<LoginDto.DefaultLoginDto> {
        sentBody = body
        onAuthenticate?.let { return it() }
        return BaseResponse(
            status = 200,
            code = 200,
            data = LoginDto.DefaultLoginDto(accessToken = "AT", refreshToken = "RT", expiresIn = expiresIn),
        )
    }
}
