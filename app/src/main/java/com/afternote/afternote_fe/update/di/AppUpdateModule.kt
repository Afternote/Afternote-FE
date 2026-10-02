package com.afternote.afternote_fe.update.di

import com.afternote.afternote_fe.BuildConfig
import com.afternote.afternote_fe.update.InstalledBuild
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** `BuildConfig` 를 읽는 자리를 한 곳으로 모은다 — 관문 자체는 주입받은 값만 본다 (#1539). */
@Module
@InstallIn(SingletonComponent::class)
object AppUpdateModule {
    @Provides
    @Singleton
    fun provideInstalledBuild(): InstalledBuild =
        InstalledBuild(
            versionCode = BuildConfig.VERSION_CODE,
            storeDistributed = BuildConfig.STORE_DISTRIBUTED_BUILD,
        )
}
