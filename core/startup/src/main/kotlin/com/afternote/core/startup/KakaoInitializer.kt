package com.afternote.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.kakao.sdk.common.KakaoSdk

/**
 * 카카오 SDK 초기화. [GlobalApplication]이 아닌 App Startup으로 실행해 Application 본문을 비운다.
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
