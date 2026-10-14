package com.afternote.afternote_fe.invitation

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 카카오톡 초대 스킴 Intent 해석 계약 (#944). `MainActivity` 가 부르는 `fromIntent(Intent)` 에
 * 진짜 [Intent] 를 넣는다 — URI 의 scheme·host·query 이름까지 계약에 포함된다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReceiverInvitationIntentContractTest {
    @Test
    fun `카카오링크 스킴의 inviteToken 을 돌려준다`() {
        val token = "Xk9_-abcDEF0123456789abcdefghijklmnopqrstuv"

        assertEquals(token, ReceiverInvitationIntentContract.fromIntent(viewIntent("kakao$APP_KEY://kakaolink?inviteToken=$token")))
    }

    @Test
    fun `데이터가 없는 launcher 진입은 초대가 아니다`() {
        assertNull(ReceiverInvitationIntentContract.fromIntent(Intent(Intent.ACTION_MAIN)))
    }

    @Test
    fun `host 가 kakaolink 가 아니면 거부한다`() {
        assertNull(ReceiverInvitationIntentContract.fromIntent(viewIntent("kakao$APP_KEY://oauth?inviteToken=abc")))
    }

    @Test
    fun `kakao 로 시작하지 않는 스킴은 거부한다`() {
        assertNull(ReceiverInvitationIntentContract.fromIntent(viewIntent("https://kakaolink?inviteToken=abc")))
    }

    @Test
    fun `토큰이 없거나 비어 있으면 거부한다`() {
        assertNull(ReceiverInvitationIntentContract.fromIntent(viewIntent("kakao$APP_KEY://kakaolink")))
        assertNull(ReceiverInvitationIntentContract.fromIntent(viewIntent("kakao$APP_KEY://kakaolink?inviteToken=")))
    }

    @Test
    fun `base64url 밖 문자나 길이 상한을 넘는 토큰은 거부한다`() {
        listOf("abc def", "abc/def", "abc+def", "한글", "a".repeat(129)).forEach { raw ->
            val encoded = Uri.encode(raw)
            assertNull(
                "'$raw' 를 받으면 안 된다",
                ReceiverInvitationIntentContract.fromIntent(viewIntent("kakao$APP_KEY://kakaolink?inviteToken=$encoded")),
            )
        }
    }

    private fun viewIntent(uri: String): Intent = Intent(Intent.ACTION_VIEW, uri.toUri())

    private companion object {
        const val APP_KEY = "0123456789abcdef"
    }
}
