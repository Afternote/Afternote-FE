package com.afternote.feature.afternote.data.di

import com.afternote.feature.afternote.data.local.DataStoreReceiverAuthCodeLocalDataSource
import com.afternote.feature.afternote.data.local.ReceiverAuthCodeLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AfternoteReceiverAuthLocalModule {
    @Binds
    @Singleton
    abstract fun bindReceiverAuthCodeLocalDataSource(impl: DataStoreReceiverAuthCodeLocalDataSource): ReceiverAuthCodeLocalDataSource
}
