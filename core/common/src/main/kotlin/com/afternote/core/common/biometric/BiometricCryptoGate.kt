package com.afternote.core.common.biometric

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

private const val LOG_TAG = "BiometricCryptoGate"
private const val KEY_ALIAS = "afternote_biometric_gate"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"

private val TRANSFORMATION =
    "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"

/**
 * 인증이 실제로 일어났음을 증명하기 위해 성공 콜백에서 돌리는 고정 평문.
 * 값 자체에는 의미가 없다 — 중요한 것은 사용자 인증에 묶인 키로 연산이 성사되는가뿐이다.
 */
private val CRYPTO_CHALLENGE = "afternote-biometric-gate".toByteArray()

/**
 * 앱 소유 `CryptoObject` 를 동반하는 인증 경로인지.
 *
 * `androidx.biometric` 의 `authenticate(promptInfo, cryptoObject)` 는 허용자에
 * `DEVICE_CREDENTIAL` 이 포함된 채 API 30 미만이면 다음 메시지로 `IllegalArgumentException` 을 던진다.
 *
 * > Crypto-based authentication is not supported for device credential prior to API 30.
 *
 * API 26~27은 기존 기기 자격 폴백을 유지한다. API 28~29는 강한 생체 인증만 허용하며
 * 강도 제한에 필요한 내부 CryptoObject는 AndroidX가 준비한다.
 * 앱 소유 CryptoObject의 사용 범위는 기존과 같이 API 30 이상으로 유지한다.
 */
val isBiometricCryptoSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

/**
 * 사용자 인증에 묶인 Keystore 키로 초기화한 [BiometricPrompt.CryptoObject] 를 만든다.
 *
 * 키 생성·초기화가 어떤 이유로든 실패하면 null 을 돌려 호출 측이 인증 자체를 포기하지 않도록 한다.
 * 이 관문은 열람 게이트일 뿐 보관 중인 암호문이 없어, 키를 쓸 수 없는 기기에서까지 로그인을
 * 막는 것은 얻는 것보다 잃는 것이 크다.
 */
fun createBiometricCryptoObject(): BiometricPrompt.CryptoObject? =
    runCatching { BiometricPrompt.CryptoObject(initEncryptCipher()) }
        .onFailure { Log.w(LOG_TAG, "CryptoObject 준비 실패 — crypto 없이 진행한다", it) }
        .getOrNull()

/**
 * 인증 성공 콜백이 받은 [cipher] 로 실제 암호 연산이 가능한지 검증한다.
 *
 * [createBiometricCryptoObject]의 앱 소유 키는 `setUserAuthenticationRequired(true)`로
 * 만들어져 있어, 프롬프트를 거치지 않고 성공 콜백만 가로채 호출하면 이 연산이 실패한다.
 * AndroidX가 강도 제한을 위해 준비하는 내부 cipher는 사용자 인증에 묶인 키가 아니므로
 * 같은 연산을 수행하더라도 그 연산 자체를 사용자 인증의 추가 증거로 간주하지 않는다.
 *
 * [cipher] 가 null 인 폴백은 검증할 대상이 없으므로 통과시킨다.
 * API 30 미만에도 AndroidX가 내부 cipher를 반환하면 동일한 암호 연산으로 검증한다.
 * 실패 원인을 호출 측이 기록할 수 있도록 예외를 삼키지 않고 [Result] 로 실어 보낸다.
 *
 * **거부는 `UserNotAuthenticatedException` 으로 오지 않는다.** API 35 실측(2026-08-26)에서
 * 인증 없이 연산했을 때 다음 모양으로 왔다.
 *
 * ```
 * javax.crypto.IllegalBlockSizeException
 *   <- android.security.KeyStoreException(Key user not authenticated, Keystore code: -26)
 * ```
 *
 * 그래서 [runCatching] 으로 전부 받는다 — 좁은 catch 로 바꾸면 그 예외가 그대로 앱을 타고 올라간다.
 */
fun confirmWithCryptoOperation(cipher: Cipher?): Result<Unit> {
    if (cipher == null) return Result.success(Unit)
    return runCatching { cipher.doFinal(CRYPTO_CHALLENGE) }.map { }
}

private fun initEncryptCipher(): Cipher {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    return try {
        cipher.apply { init(Cipher.ENCRYPT_MODE, loadOrCreateKey()) }
    } catch (e: KeyPermanentlyInvalidatedException) {
        // 생체 정보가 새로 등록되면 기존 키가 영구 무효화된다.
        // 이 키는 잠금 해제 관문 전용이라 이 키로 보호 중인 데이터가 없으므로 폐기 후 재생성한다.
        Log.i(LOG_TAG, "생체 등록 변경으로 키가 무효화되어 재생성한다", e)
        deleteKey()
        cipher.apply { init(Cipher.ENCRYPT_MODE, loadOrCreateKey()) }
    }
}

private fun loadOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

    val spec =
        KeyGenParameterSpec
            .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // 0초 = 매 사용마다 인증을 요구한다. 허용자는 프롬프트 정책과 같은 폭으로 맞춘다.
                    setUserAuthenticationParameters(
                        0,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                    )
                }
            }.build()

    return KeyGenerator
        .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        .apply { init(spec) }
        .generateKey()
}

private fun deleteKey() {
    runCatching {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
    }.onFailure { Log.w(LOG_TAG, "무효화된 키 삭제 실패", it) }
}
