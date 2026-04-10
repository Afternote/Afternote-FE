package com.afternote.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.kakao.sdk.common.KakaoSdk

/**
 * 카카오 SDK 초기화. `Application`이 아닌 App Startup으로 실행한다.
 *
 * `app` 모듈의 [com.afternote.afternote_fe.BuildConfig]는 라이브러리에서 참조할 수 없으므로
 * [BuildConfig]는 `core:startup`의 `build.gradle.kts`에서 동일 키로 생성한다.
 */
class KakaoInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        KakaoSdk.init(
            context.applicationContext,
            BuildConfig.KAKAO_NATIVE_APP_KEY,
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
