package com.afternote.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.afternote.core.datastore.userProfilePreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UserProfileDataStoreModule {
    @Provides
    @Singleton
    @UserProfileDataStore
    fun provideUserProfileDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.userProfilePreferencesDataStore
}
