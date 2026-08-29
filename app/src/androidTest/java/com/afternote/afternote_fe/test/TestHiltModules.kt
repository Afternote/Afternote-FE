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
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.domain.testing.FakeWeeklyReportRepository
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
     * 실제 구현은 네트워크를 탄다 — 홈이 진입 시 주간 기록 수를 부르고(#562) 주간리포트 탭도
     * 합성되기만 하면 이 경로를 지나므로, 실제 구현으로 두면 계측이 실서버에 붙는다. CI stub
     * 응답이 TokenAuthenticator 를 타고 FakeAuthRepository 로 흘러들어 인증 테스트가 깨졌다.
     *
     * 정본 fixture 를 쓴다 — 시나리오마다 클래스를 새로 만들면 계약이 바뀔 때 고칠 곳이 갈라진다
     * (#936 · #1022 · #1030 의 androidTest 컴파일 파손 원인). 요청한 주차가 `requestedDates` 에
     * 남아 「열지 않은 탭은 부르지 않는다」(#736) 를 횟수로 단언할 수 있다.
     *
     * 응답을 큐에 넣지 않은 테스트에는 **실패**를 돌려준다 — 주간 수는 실패를 0 으로 접지 않고
     * 미상(대시)으로 그리므로(#562), 그 화면이 홈의 다른 단언을 방해하지 않는다.
     */
    @Provides
    @Singleton
    fun provideWeeklyReportRepository(): WeeklyReportRepository =
        FakeWeeklyReportRepository(
            whenQueueEmpty = Result.failure(IllegalStateException("주간 리포트 응답이 큐에 없다 — 테스트가 값을 넣지 않았다")),
        )
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
