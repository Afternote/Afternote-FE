package com.afternote.afternote_fe

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 초기화 로직은 전부 core:startup의 App Startup Initializer로 둔다.
    }
}
