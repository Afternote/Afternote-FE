package com.afternote.feature.afternote.presentation.receiver.detail

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.receiver.domain.model.ReceivedAccountCredentials
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SOCIAL 수신 상세가 계정 자격증명의 **부재를 부재로** 옮긴다는 계약 (이슈 #619).
 *
 * 서버(`GET receiver-auth/after-notes/{id}`)는 수신자에게 `credentials` 를 내려주지 않는다.
 * 그런데도 매퍼가 `orEmpty()` 로 빈 문자열을 채우던 탓에 화면이 빈 비밀번호에 마스킹을 그렸고,
 * 수신자는 "표시" 를 눌러 빈 값을 확인하고서야 아무것도 전달되지 않았음을 알았다.
 */
class ReceivedSocialNetworkCredentialsMappingTest {
    private fun detail(credentials: ReceivedAccountCredentials?) =
        ReceivedAfternoteDetail(
            serviceName = "인스타그램",
            senderName = "홍길동",
            type = AfternoteType.SOCIAL_NETWORK,
            credentials = credentials,
        )

    private fun credentialsOf(detail: ReceivedAfternoteDetail): ReceivedAccountCredentialsUiModel? {
        val uiModel = detail.toReceivedDetailContentUiModel()

        return (uiModel as ReceivedDetailContentUiModel.SocialNetwork).content.credentials
    }

    @Test
    fun `서버가 credentials 를 안 주면 표시 모델도 없다`() {
        assertNull(credentialsOf(detail(credentials = null)))
    }

    @Test
    fun `아이디와 비밀번호가 모두 비면 표시 모델을 만들지 않는다`() {
        val bothMissing = ReceivedAccountCredentials(id = null, password = null)

        assertNull(credentialsOf(detail(bothMissing)))
    }

    @Test
    fun `공백뿐인 값은 미제공과 같게 다룬다`() {
        val blank = ReceivedAccountCredentials(id = "", password = "   ")

        assertNull(credentialsOf(detail(blank)))
    }

    @Test
    fun `한쪽만 남겼으면 그쪽만 값으로 싣는다`() {
        val idOnly = credentialsOf(detail(ReceivedAccountCredentials(id = "qwerty123", password = null)))

        assertEquals("qwerty123", idOnly?.accountId)
        assertNull("비밀번호를 남기지 않았는데 값이 생겼다", idOnly?.password)

        val passwordOnly = credentialsOf(detail(ReceivedAccountCredentials(id = " ", password = "qwerty123!")))

        assertNull("아이디가 공백뿐인데 값으로 실렸다", passwordOnly?.accountId)
        assertEquals("qwerty123!", passwordOnly?.password)
    }

    @Test
    fun `둘 다 남겼으면 그대로 싣는다`() {
        val both = credentialsOf(detail(ReceivedAccountCredentials(id = "qwerty123", password = "qwerty123!")))

        assertEquals("qwerty123", both?.accountId)
        assertEquals("qwerty123!", both?.password)
    }
}
