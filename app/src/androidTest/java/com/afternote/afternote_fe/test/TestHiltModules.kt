package com.afternote.afternote_fe.test

import com.afternote.afternote_fe.notification.NotificationPermissionRequestStore
import com.afternote.afternote_fe.notification.di.NotificationPermissionStoreModule
import com.afternote.afternote_fe.reporting.ErrorReportingModule
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.data.di.CoreUserRepositoryModule
import com.afternote.core.domain.repository.UserProfileRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeUserProfileRepository
import com.afternote.feature.mindrecord.data.di.MindRecordRepositoryModule
import com.afternote.feature.mindrecord.data.repositoryimpl.MindRecordReceiverRepositoryImpl
import com.afternote.feature.mindrecord.data.repositoryimpl.WeeklyReportRepositoryImpl
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreUserRepositoryModule::class],
)
object TestCoreUserRepositoryModule {
    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = appTestAuthRepository(loggedIn = false)

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = appTestUserRepository()

    @Provides
    @Singleton
    fun provideUserProfileRepository(): UserProfileRepository = FakeUserProfileRepository()
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NotificationPermissionStoreModule::class],
)
object TestNotificationPermissionStoreModule {
    /**
     * 계측에서는 알림 권한을 묻지 않는다 (#1454).
     *
     * API 33+ managed device(api34·api36)에서 로그인 상태를 만드는 계측이 화면을 검사하는 동안
     * 시스템 권한 다이얼로그가 올라오면 그 테스트가 통째로 깨진다. 이미 물어본 기기로 고정해
     * 요청 자체를 일으키지 않는다 — 요청 경로의 판정은 단위 테스트와 adb 실측이 맡는다.
     */
    @Provides
    @Singleton
    fun provideNotificationPermissionRequestStore(): NotificationPermissionRequestStore =
        object : NotificationPermissionRequestStore {
            override val hasRequested = flowOf(true)

            override suspend fun markRequested() = Unit
        }
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
