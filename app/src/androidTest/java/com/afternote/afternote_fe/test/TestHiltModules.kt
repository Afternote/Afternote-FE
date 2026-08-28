package com.afternote.afternote_fe.test

import com.afternote.afternote_fe.reporting.ErrorReportingModule
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.data.di.CoreUserRepositoryModule
import com.afternote.core.domain.repository.UserProfileRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeUserProfileRepository
import com.afternote.feature.mindrecord.data.di.MindRecordRepositoryModule
import com.afternote.feature.mindrecord.data.repositoryimpl.MindRecordReceiverRepositoryImpl
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
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

    /**
     * 주간 리포트도 fake 로 격리한다.
     *
     * 홈이 진입 시 주간 기록 수를 부르게 되면서(#562), 이 바인딩만 실제 구현으로 남아 있으면
     * 로그인·홈 진입 계측 테스트가 `/mind-record` 를 실제로 친다. CI stub 응답이
     * TokenAuthenticator 를 타고 FakeAuthRepository 로 흘러들어 인증 테스트가 깨졌다.
     *
     * 응답을 큐에 넣지 않은 테스트에는 **실패**를 돌려준다 — 주간 수는 실패를 0 으로 접지 않고
     * 미상(대시)으로 그리므로(#562), 그 화면이 홈의 다른 단언을 방해하지 않는다.
     */
    @Provides
    @Singleton
    fun provideWeeklyReportRepository(): WeeklyReportRepository = TestWeeklyReportRepository
}

object TestWeeklyReportRepository : WeeklyReportRepository {
    val results = ArrayDeque<Result<WeeklyReport>>()
    val requestedDates = mutableListOf<String>()

    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
        requestedDates += date
        return results.removeFirstOrNull()
            ?: Result.failure(IllegalStateException("주간 리포트 응답이 큐에 없다 — 테스트가 값을 넣지 않았다"))
    }
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
