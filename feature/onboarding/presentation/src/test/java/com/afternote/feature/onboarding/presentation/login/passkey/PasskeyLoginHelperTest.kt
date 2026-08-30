package com.afternote.feature.onboarding.presentation.login.passkey

import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.UnknownError
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import com.afternote.core.domain.error.CoreAuthFailure
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Credential Manager 경계의 옵션 조립·응답 파싱·실패 갈래 회귀 가드 (#764).
 *
 * `CredentialManager` 실물은 프레임워크 콜백 API 를 통째로 채워야 흉내 낼 수 있어, 경계를
 * `getCredential` 함수형 파라미터 하나로 끊고 그쪽을 fake 로 바꾼다. `GetPublicKeyCredentialOption`
 * 과 `PublicKeyCredential` 은 내부에서 `Bundle` 을 만들므로 Robolectric 이 필요하다.
 *
 * **여기서 닫히지 않는 것** — 시스템 선택기가 실제로 뜨는지, 서버가 assertion 을 받아들이는지는
 * 실기기·실서버 몫이다(Afternote/Afternote-BE#272 로 막혀 있다).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PasskeyLoginHelperTest {
    private val requestJson = """{"challenge":"abc","rpId":"afternote.kro.kr","userVerification":"required"}"""

    @Test
    fun `요청에 패스키 옵션과 서버 원문을 그대로 싣는다`() {
        var captured: GetCredentialRequest? = null

        runBlocking {
            requestPasskeyAssertion(requestJson) { request ->
                captured = request
                GetCredentialResponse(PublicKeyCredential("""{"id":"cid"}"""))
            }
        }

        val option = captured?.credentialOptions?.single()
        assertTrue(option is GetPublicKeyCredentialOption)
        assertEquals(requestJson, (option as GetPublicKeyCredentialOption).requestJson)
    }

    @Test
    fun `자격이 즉시 없으면 시스템 안내 없이 돌아오도록 요청한다`() {
        // 사용자가 누른 적 없는 자동 시도라, 자격이 없을 때 시스템이 대신 시트를 띄우면 안 된다.
        var captured: GetCredentialRequest? = null

        runBlocking {
            requestPasskeyAssertion(requestJson) { request ->
                captured = request
                GetCredentialResponse(PublicKeyCredential("""{"id":"cid"}"""))
            }
        }

        assertTrue(captured!!.preferImmediatelyAvailableCredentials)
    }

    @Test
    fun `응답 파싱 - assertion 원문을 그대로 돌려준다`() {
        val assertion = """{"id":"cid","type":"public-key","response":{"signature":"sig"}}"""

        val result =
            runBlocking {
                requestPasskeyAssertion(requestJson) { GetCredentialResponse(PublicKeyCredential(assertion)) }
            }

        assertEquals(assertion, result.getOrThrow())
    }

    @Test
    fun `패스키가 아닌 자격이 오면 조용히 흘리지 않고 실패로 드러낸다`() {
        val result =
            runBlocking {
                requestPasskeyAssertion(requestJson) {
                    GetCredentialResponse(CustomCredential("some.other.TYPE", Bundle()))
                }
            }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `사용자 취소 - 도메인 취소 타입으로 옮긴다`() {
        val result =
            runBlocking {
                requestPasskeyAssertion(requestJson) { throw GetCredentialCancellationException() }
            }

        assertTrue(result.exceptionOrNull() is CoreAuthFailure.UserCancelledAuth)
    }

    @Test
    fun `이 기기에 패스키가 없으면 NoCredentialException 을 그대로 넘긴다`() {
        // 호출부가 "예정된 결말" 로 소비할 수 있어야 한다 — 다른 타입으로 감싸면 진짜 장애와 섞인다.
        val noCredential = NoCredentialException()

        val result = runBlocking { requestPasskeyAssertion(requestJson) { throw noCredential } }

        assertSame(noCredential, result.exceptionOrNull())
    }

    @Test
    fun `그 밖의 Credential Manager 실패는 원본 그대로 넘긴다`() {
        val unknown = GetCredentialUnknownException("provider crashed")

        val result = runBlocking { requestPasskeyAssertion(requestJson) { throw unknown } }

        assertSame(unknown, result.exceptionOrNull())
    }

    @Test
    fun `서버 원문이 표준에 맞지 않아 인증기가 거절하면 그 실패가 드러난다`() {
        val domException = GetPublicKeyCredentialDomException(UnknownError(), "bad options")

        val result = runBlocking { requestPasskeyAssertion(requestJson) { throw domException } }

        assertSame(domException, result.exceptionOrNull())
    }
}
