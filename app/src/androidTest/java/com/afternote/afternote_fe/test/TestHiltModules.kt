package com.afternote.afternote_fe.test

import com.afternote.afternote_fe.notification.NotificationPermissionRequestStore
import com.afternote.afternote_fe.notification.di.NotificationPermissionStoreModule
import com.afternote.afternote_fe.reporting.ErrorReportingModule
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.data.di.CoreUserRepositoryModule
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
import com.afternote.feature.mindrecord.data.di.MindRecordRepositoryModule
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.domain.testing.FakeMindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.testing.FakeWeeklyReportRepository
import com.afternote.feature.timeletter.data.di.TimeLetterModule
import com.afternote.feature.timeletter.data.repositoryImpl.FileMetadataRepositoryImpl
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.domain.repository.VoiceRecorderRepository
import com.afternote.feature.timeletter.domain.testing.FakeReceiverTimeLetterRepository
import com.afternote.feature.timeletter.domain.testing.FakeTimeLetterRepository
import com.afternote.feature.timeletter.domain.testing.FakeVoiceRecorderRepository
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

    // 책임별 좁은 계약 2종 (#1282). 이 모듈이 `CoreUserRepositoryModule` 을 통째로 replaces 하므로
    // 거기 있던 두 바인딩도 같이 사라진다 — 좁은 계약을 주입받는 화면(#1743 의 애프터노트 상세·에디터)이
    // 계측 그래프에서 MissingBinding 으로 깨지지 않게 여기서 다시 잇는다.
    // 프로덕션과 같은 모양으로 싱글턴 `UserRepository` 를 경유해, 계측이 세운 fake 한 인스턴스를 공유한다.
    @Provides
    fun provideUserReceiverRepository(userRepository: UserRepository): UserReceiverRepository = userRepository

    @Provides
    fun provideMyProfileRepository(userRepository: UserRepository): MyProfileRepository = userRepository

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
 * 두 fake 는 일부러 `strict()` 를 쓰지 않는다. 앱 전체 계측에서 관심 밖 탭이 합성돼도 무인자
 * 기본값의 빈 목록 성공으로 닫기 위해서다. 기본 생성자 정책이 strict 로 바뀌면 이 바인딩도
 * 함께 재검토해야 한다.
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

    /**
     * 녹음도 fake 로 격리한다.
     *
     * 실제 구현은 `MediaRecorder` 로 마이크를 잡는다 — 앱 전체를 띄우는 계측에서 실제 녹음 장치에
     * 붙이면 에뮬레이터 환경에 따라 흔들리고, 이 저장소를 명시적으로 다루는 테스트는
     * [VoiceRecordingAndroidTest]·[VoiceRecordingLifecycleAndroidTest] 처럼
     * `createVoiceRecorderRepositoryForTesting` 로 실제 구현을 직접 얻어 검증한다.
     *
     * `start`/`stop` 은 일부러 [FakeVoiceRecorderRepository] 의 기본대로 호출 시 터진다 — 관심 밖
     * 화면이 실수로 녹음을 시작하면 조용히 넘어가지 않고 바로 드러나야 한다.
     */
    @Provides
    @Singleton
    fun provideVoiceRecorderRepository(): VoiceRecorderRepository = FakeVoiceRecorderRepository
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
