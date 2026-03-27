package com.afternote.core.common.di

import com.afternote.core.data.repositoryImpl.auth.AccountRepositoryImpl
import com.afternote.core.data.repositoryImpl.auth.AuthRepositoryImpl
import com.afternote.core.domain.repository.AccountRepository
import com.afternote.core.domain.repository.AuthRepository
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
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    companion object {
        @Provides
        @Singleton
        fun provideKakaoTokenManageable(): TokenManageable = TokenManagerProvider.instance.manager
    }
}
