package com.afternote.feature.afternote.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 수신자 인증 전용 prefs 파일 이름. 다른 용도로 동일 이름을 쓰지 않습니다. */
private const val PREFS_NAME = "afternote_receiver_auth"

private const val KEY_AUTH_CODE = "receiver_auth_code"

/**
 * 일반 [SharedPreferences]로 [ReceiverAuthCodeLocalDataSource]를 구현합니다.
 *
 * 저장은 `SharedPreferences.Editor.apply()`로 비동기 반영됩니다. 비밀이 아니거나 민감도가 낮은 코드에 적합합니다.
 * 저장소 암호화가 필요하면 인터페이스는 그대로 두고 구현만 EncryptedSharedPreferences로 교체하면 됩니다.
 */
@Singleton
class SharedPreferencesReceiverAuthCodeLocalDataSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ReceiverAuthCodeLocalDataSource {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        /** 저장된 코드를 반환합니다. 없거나 공백만 있으면 null입니다. */
        override fun getSavedCode(): String? = prefs.getString(KEY_AUTH_CODE, null)?.takeIf { it.isNotBlank() }

        /** `code`를 trim한 뒤 저장합니다. 비어 있으면 [clearCode]와 같이 저장소를 비웁니다. */
        override fun saveCode(code: String) {
            val trimmed = code.trim()
            if (trimmed.isEmpty()) {
                clearCode()
            } else {
                prefs.edit().putString(KEY_AUTH_CODE, trimmed).apply()
            }
        }

        /** 저장된 코드를 삭제합니다. 로그아웃·초기화 또는 빈 문자열 [saveCode] 호출 때 사용합니다. */
        override fun clearCode() {
            prefs.edit().remove(KEY_AUTH_CODE).apply()
        }
    }
