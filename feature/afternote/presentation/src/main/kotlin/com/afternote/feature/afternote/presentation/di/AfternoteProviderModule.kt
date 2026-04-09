package com.afternote.feature.afternote.presentation.di

import com.afternote.core.common.dev.DevEnvironment
import com.afternote.feature.afternote.presentation.author.editor.provider.AfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.editor.provider.FakeAfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.editor.provider.RealAfternoteEditorDataProvider
import com.afternote.feature.afternote.presentation.author.editor.receiver.provider.FakeReceiverDataProvider
import com.afternote.feature.afternote.presentation.author.editor.receiver.provider.RealReceiverDataProvider
import com.afternote.feature.afternote.presentation.author.editor.receiver.provider.ReceiverDataProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds editor/receiver UI data providers; [DevEnvironment.USE_FAKE_DATA] selects fake vs real. */
@Module
@InstallIn(SingletonComponent::class)
object AfternoteProviderModule {
    @Provides
    @Singleton
    fun provideAfternoteEditorDataProvider(
        fake: FakeAfternoteEditorDataProvider,
        real: RealAfternoteEditorDataProvider,
    ): AfternoteEditorDataProvider = if (DevEnvironment.USE_FAKE_DATA) fake else real

    @Provides
    @Singleton
    fun provideReceiverDataProvider(
        fake: FakeReceiverDataProvider,
        real: RealReceiverDataProvider,
    ): ReceiverDataProvider = if (DevEnvironment.USE_FAKE_DATA) fake else real
}
