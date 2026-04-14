package com.afternote.core.di

import com.afternote.core.data.repoimpl.HomeRepositoryImpl
import com.afternote.core.data.repoimpl.PhotoUploadRepositoryImpl
import com.afternote.core.data.repoimpl.account.AccountRepositoryImpl
import com.afternote.core.data.repoimpl.auth.AuthRepositoryImpl
import com.afternote.core.data.repoimpl.auth.GoogleAuthManagerImpl
import com.afternote.core.data.repoimpl.auth.KakaoAuthManagerImpl
import com.afternote.core.domain.repository.HomeRepository
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.repository.auth.GoogleAuthManager
import com.afternote.core.domain.repository.auth.KakaoAuthManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface CoreRepositoryModule {
    @Binds
    @Singleton
    fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository

    @Binds
    @Singleton
    fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    fun bindPhotoUploadRepository(impl: PhotoUploadRepositoryImpl): PhotoUploadRepository

    @Binds
    @Singleton
    fun bindKakaoAuthManager(impl: KakaoAuthManagerImpl): KakaoAuthManager

    @Binds
    @Singleton
    fun bindGoogleAuthManager(impl: GoogleAuthManagerImpl): GoogleAuthManager

    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
