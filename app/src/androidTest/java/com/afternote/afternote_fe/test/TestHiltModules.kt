package com.afternote.afternote_fe.test

import com.afternote.afternote_fe.notification.NotificationPermissionRequestStore
import com.afternote.afternote_fe.notification.di.NotificationPermissionStoreModule
import com.afternote.afternote_fe.reporting.ErrorReportingModule
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.data.di.CoreUserRepositoryModule
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
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
    fun provideUserProfileCacheRepository(): UserProfileCacheRepository = FakeUserProfileCacheRepository()
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
     * **`fallback` 을 준다 — 큐가 비어도 터뜨리지 않는다.** 정본 fixture 의 기본은 「큐가 비면 실패」
     * 이고 화면 단위 테스트는 그게 맞다. 그런데 앱 전체를 띄우는 계측은 **홈을 지나가기만 해도** 이
     * 저장소를 부르고, 그 횟수는 화면 전환·`ON_RESUME` 에 따라 달라져 `@Before` 에서 미리 셀 수 없다.
     * 큐만 두면 두 번째 호출에서 터져 **홈이 못 뜨고, 실패는 「인사말이 안 보인다」 같은 엉뚱한
     * 자리에서 나타난다** — 실제로 `mode: full` 계측에서 그렇게 나왔다 (#562 · #1288).
     *
     * **세는 단언은 그대로다.** `fallback` 을 줘도 `requestedDates` 는 계속 쌓이므로 「열지 않은 탭은
     * 부르지 않는다」(#736) 를 횟수로 단언할 수 있고, 특정 응답이 필요한 테스트는 종전대로 큐에 넣는다.
     */
    @Provides
    @Singleton
    fun provideWeeklyReportRepository(): WeeklyReportRepository = FakeWeeklyReportRepository(fallback = Result.success(emptyWeeklyReport()))
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
