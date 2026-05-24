package com.afternote.afternote_fe

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.afternote.core.network.di.CoilImageLoaderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GlobalApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        EntryPointAccessors
            .fromApplication(context, CoilImageLoaderEntryPoint::class.java)
            .imageLoader()

    override fun onCreate() {
        super.onCreate()
        // 초기화 로직은 전부 core:startup의 App Startup Initializer로 둔다.
    }
}
