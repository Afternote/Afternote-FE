package com.afternote.afternote_fe

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.afternote.afternote_fe.messaging.FcmNotificationChannel
import com.afternote.core.network.di.CoilImageLoaderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication :
    Application(),
    SingletonImageLoader.Factory {
    /** Hilt 가 만든 [ImageLoader] 를 Coil 앱 전역 싱글톤으로 등록 — 모든 AsyncImage 가 명시적 imageLoader 없이 이걸 사용. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        EntryPointAccessors
            .fromApplication(context, CoilImageLoaderEntryPoint::class.java)
            .imageLoader()

    override fun onCreate() {
        super.onCreate()
        FcmNotificationChannel.create(this)
        // 공통 초기화는 core:startup에 두고, app 전용 Firebase 채널만 여기서 생성한다.
    }
}
