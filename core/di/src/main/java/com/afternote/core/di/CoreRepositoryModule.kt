package com.afternote.core.di

import android.util.Log
import com.afternote.core.common.dev.DevEnvironment
import com.afternote.core.data.repositoryImpl.PhotoUploadRepositoryImpl
import com.afternote.core.data.repositoryImpl.account.AccountRepositoryImpl
import com.afternote.core.data.repositoryImpl.auth.AuthRepositoryImpl
import com.afternote.core.data.repositoryImpl.auth.KakaoAuthManagerImpl
import com.afternote.core.data.repositoryImpl.stub.auth.FakeAuthRepository
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.repository.auth.KakaoAuthManager
import com.kakao.sdk.auth.TokenManageable
import com.kakao.sdk.auth.TokenManagerProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface CoreRepositoryModule {
    @Binds
    @Singleton
    fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    fun bindPhotoUploadRepository(impl: PhotoUploadRepositoryImpl): PhotoUploadRepository

    @Binds
    @Singleton
    fun bindKakaoAuthManager(impl: KakaoAuthManagerImpl): KakaoAuthManager

    companion object {
        private const val TAG = "AuthRepositorySwitch"

        @Provides
        @Singleton
        fun provideAuthRepository(
            fakeImpl: FakeAuthRepository,
            realImpl: AuthRepositoryImpl,
        ): AuthRepository =
            if (DevEnvironment.USE_FAKE_DATA) {
                Log.d(TAG, "가짜 데이터 모드: FakeAuthRepository 주입")
                fakeImpl
            } else {
                Log.d(TAG, "실제 서버 연동 모드: AuthRepositoryImpl 주입")
                realImpl
            }

        @Provides
        @Singleton
        fun provideKakaoTokenManageable(): TokenManageable = TokenManagerProvider.instance.manager
    }
}
