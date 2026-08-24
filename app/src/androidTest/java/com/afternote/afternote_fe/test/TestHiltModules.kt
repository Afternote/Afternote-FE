package com.afternote.afternote_fe.test

import com.afternote.afternote_fe.reporting.ErrorReportingModule
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.data.repoimpl.PhotoUploadRepositoryImpl
import com.afternote.core.data.repoimpl.UserProfileRepositoryImpl
import com.afternote.core.data.repoimpl.VideoUploadRepositoryImpl
import com.afternote.core.data.repoimpl.account.AccountRepositoryImpl
import com.afternote.core.di.CoreRepositoryModule
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.UserProfileRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.VideoUploadRepository
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.feature.mindrecord.data.di.MindRecordRepositoryModule
import com.afternote.feature.mindrecord.data.repositoryimpl.MindRecordReceiverRepositoryImpl
import com.afternote.feature.mindrecord.data.repositoryimpl.WeeklyReportRepositoryImpl
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreRepositoryModule::class],
)
object TestCoreRepositoryModule {
    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = FakeAuthRepository(loggedIn = false)

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = FakeUserRepository()

    @Provides
    @Singleton
    fun provideUserProfileRepository(): UserProfileRepository = FakeUserProfileRepository()

    @Provides
    @Singleton
    fun provideAccountRepository(impl: AccountRepositoryImpl): AccountRepository = impl

    @Provides
    @Singleton
    fun providePhotoUploadRepository(impl: PhotoUploadRepositoryImpl): PhotoUploadRepository = impl

    @Provides
    @Singleton
    fun provideVideoUploadRepository(impl: VideoUploadRepositoryImpl): VideoUploadRepository = impl
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MindRecordRepositoryModule::class],
)
object TestMindRecordRepositoryModule {
    @Provides
    @Singleton
    fun provideDiaryRepository(): DiaryRepository = FakeDiaryRepository()

    @Provides
    @Singleton
    fun provideDailyQuestionRepository(): DailyQuestionRepository = FakeDailyQuestionRepository()

    @Provides
    @Singleton
    fun provideMindRecordReceiverRepository(impl: MindRecordReceiverRepositoryImpl): MindRecordReceiverRepository = impl

    @Provides
    @Singleton
    fun provideWeeklyReportRepository(impl: WeeklyReportRepositoryImpl): WeeklyReportRepository = impl
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ErrorReportingModule::class],
)
object TestErrorReportingModule {
    @Provides
    @Singleton
    fun provideErrorReporter(): ErrorReporter = FakeErrorReporter()
}
