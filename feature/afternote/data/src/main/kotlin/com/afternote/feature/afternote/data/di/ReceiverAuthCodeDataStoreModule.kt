package com.afternote.feature.afternote.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.afternote.feature.afternote.data.local.receiverAuthCodePreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ReceiverAuthCodeDataStoreModule {
    @Provides
    @Singleton
    @ReceiverAuthCodeDataStore
    fun provideReceiverAuthCodeDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.receiverAuthCodePreferencesDataStore
}
