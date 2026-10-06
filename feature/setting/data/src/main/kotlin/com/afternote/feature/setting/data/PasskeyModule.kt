package com.afternote.feature.setting.data

import com.afternote.feature.setting.domain.PasskeyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
internal object PasskeyModule {
    @Provides
    fun providePasskeyRepository(retrofit: Retrofit): PasskeyRepository = PasskeyRepositoryImpl(retrofit.create<PasskeyApiService>())
}
