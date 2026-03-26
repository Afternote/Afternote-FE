package com.afternote.feature.onboarding.data.di

import com.afternote.feature.onboarding.data.repositoryimpl.LoginRepositoryImpl
import com.afternote.feature.onboarding.data.service.LoginApiService
import com.afternote.feature.onboarding.domain.repository.LoginRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoginRepositoryModule {
    @Suppress("unused")
    @Binds
    @Singleton
    abstract fun bindLoginRepository(impl: LoginRepositoryImpl): LoginRepository

    companion object {
        @Provides
        @Singleton
        fun provideLoginApiService(retrofit: Retrofit): LoginApiService =
            retrofit.create(LoginApiService::class.java)
    }
}
