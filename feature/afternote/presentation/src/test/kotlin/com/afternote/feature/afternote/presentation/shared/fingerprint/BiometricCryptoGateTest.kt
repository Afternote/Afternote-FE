package com.afternote.feature.afternote.presentation.shared.fingerprint

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

/**
 * [confirmWithCryptoOperation] 회귀 가드.
 *
 * 이 관문의 요점은 `onAuthenticationSucceeded` 가 왔다는 사실이 아니라 **암호 연산이 성사되는가**다
 * (CodeQL `java/android/insecure-local-authentication`). 성공 콜백만 가로챈 호출은 인증에 묶인
 * 키를 쓸 수 없어 `doFinal` 에서 떨어져야 하고, 그 실패가 성공으로 새면 관문이 없는 것과 같다.
 *
 * Keystore 자체는 JVM 단위 테스트에서 기동하지 않으므로, 여기서는 «연산이 성립하지 않는 cipher 는
 * 실패로 판정되는가» 라는 이 함수의 계약만 고정한다 — 키가 인증에 묶여 있는지는 기기 검증의 몫이다.
 */
class BiometricCryptoGateTest {
    @Test
    fun `초기화된 cipher 로 연산이 성사되면 확정된다`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }

        assertTrue(confirmWithCryptoOperation(cipher).isSuccess)
    }

    @Test
    fun `연산이 불가능한 cipher 는 성공 콜백이 와도 실패로 판정한다`() {
        // init 되지 않은 cipher 는 doFinal 에서 IllegalStateException 을 던진다.
        // 인증에 묶인 Keystore 키를 인증 없이 쓸 때의 UserNotAuthenticatedException 과 같은 자리다.
        val uninitialized = Cipher.getInstance("AES/GCM/NoPadding")

        assertFalse(confirmWithCryptoOperation(uninitialized).isSuccess)
    }

    @Test
    fun `연산 실패는 삼켜지지 않고 원인이 함께 실려 온다`() {
        val uninitialized = Cipher.getInstance("AES/GCM/NoPadding")

        assertTrue(confirmWithCryptoOperation(uninitialized).exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `cipher 가 없는 폴백 경로는 검증 대상이 없어 통과시킨다`() {
        // API 26~29 는 DEVICE_CREDENTIAL 허용자와 CryptoObject 를 함께 쓸 수 없어 cipher 가 없다.
        assertTrue(confirmWithCryptoOperation(null).isSuccess)
    }
}
