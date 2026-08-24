package com.afternote.afternote_fe.startup

import android.content.Context
import androidx.startup.Initializer
import com.afternote.afternote_fe.BuildConfig
import com.kakao.sdk.common.KakaoSdk

/**
 * 카카오 SDK 초기화. `Application`이 아닌 App Startup으로 실행한다.
 *
 * [GlobalApplication][com.afternote.afternote_fe.GlobalApplication]의 `onCreate()`를 초기화 코드로
 * 채우지 않고, 기동 시 실행할 일을 Initializer 단위로 나눠 둔다.
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
