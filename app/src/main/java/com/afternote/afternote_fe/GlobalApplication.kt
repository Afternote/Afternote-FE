package com.afternote.afternote_fe

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication : Application() {
    override fun onCreate() {
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        super.onCreate()
    }
}
