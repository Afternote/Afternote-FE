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
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.domain.testing.FakeMindRecordReceiverRepository
import com.afternote.feature.timeletter.data.di.TimeLetterModule
import com.afternote.feature.timeletter.data.repositoryImpl.FileMetadataRepositoryImpl
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.domain.testing.FakeReceiverTimeLetterRepository
import com.afternote.feature.timeletter.domain.testing.FakeTimeLetterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.flowOf
import java.io.IOException
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
    fun provideMindRecordReceiverRepository(): MindRecordReceiverRepository = FakeMindRecordReceiverRepository()

    /**
     * 정본 fake 는 응답 큐가 비면 예외를 던진다 — 이 자리에서 그러면 닫아 둔 화면에 닿았을 때
     * 앱이 죽는다(#1288). 계약만 닫고 실패로 끝낸다.
     */
    @Provides
    @Singleton
    fun provideWeeklyReportRepository(): WeeklyReportRepository =
        object : WeeklyReportRepository {
            override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> = Result.failure(IOException("계측에서 주간 리포트는 닫혀 있다"))
        }
}

/**
 * 타임레터 탭이 dev 서버로 나가는 것을 막는다.
 *
 * [NotificationNavigationAndroidTest] 의 두 테스트가 하단 탭에서 타임레터를 연다. 그 화면은
 * `TimeletterScreen` 이 `repeatOnLifecycle(STARTED)` 안에서 `TimeletterViewModel.load()` 를 부르고,
 * 그 안에서 `TimeLetterRepository.getTimeLetters()` 가 실제 `GET /api/v1/time-letters` 를 보낸다.
 * api30 GMD 실측에서 요청 3건이 확인됐다 — 탭 진입에 1건, 그리고 Activity 를 `CREATED` 로 내렸다
 * 올리는 테스트가 `STARTED` 재진입으로 1건을 더 만들어 2건. 그런데도 36개 테스트가 전부 통과했다.
 * 실패로 드러나지 않으니 dev 서버가 닫히는 03~12시(KST)에만 증상이 나오고, 그때는 원인이 코드로
 * 보이지 않는다.
 *
 * `FileMetadataRepository` 는 `ContentResolver` 만 쓰는 로컬 구현이라 그대로 둔다 — 네트워크가
 * 아닌 것까지 갈아끼우면 계측이 프로덕션과 달라지는 자리만 늘어난다.
 *
 * api service 두 개를 다시 제공하지 않는 이유는 [TimeLetterModule] 밖에 소비자가 없어서다.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [TimeLetterModule::class],
)
object TestTimeLetterModule {
    @Provides
    @Singleton
    fun provideTimeLetterRepository(): TimeLetterRepository = FakeTimeLetterRepository()

    @Provides
    @Singleton
    fun provideReceiverTimeLetterRepository(): ReceiverTimeLetterRepository = FakeReceiverTimeLetterRepository()

    @Provides
    @Singleton
    fun provideFileMetadataRepository(impl: FileMetadataRepositoryImpl): FileMetadataRepository = impl
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
