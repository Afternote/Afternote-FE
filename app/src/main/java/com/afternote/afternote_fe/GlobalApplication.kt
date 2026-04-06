package com.afternote.afternote_fe

import android.app.Application

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
// TODO:KakaoSdk.init(this, "{네이티브_APP_KEY}")
    }
}
