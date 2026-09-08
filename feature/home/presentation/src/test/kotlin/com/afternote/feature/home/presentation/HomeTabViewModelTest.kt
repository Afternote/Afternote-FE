package com.afternote.feature.home.presentation

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.User
import com.afternote.feature.home.presentation.usecase.GetHomeSummaryUseCase
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.usecase.GetWeeklyRecordCountUseCase
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [HomeTabViewModel] 홈 요약 로드의 상태·경합 계약 회귀 가드 (#795).
 *
 * [GetHomeSummaryUseCase]는 실물을 사용하고 Repository만 손으로 쓴 fake로 교체한다. 그래서
 * 네 병렬 조회의 결과가 [HomeTabUiState]로 매핑되는 경로와, 사용자 요청이 자동 갱신을
 * 선점하는 [HomeTabViewModel]의 Job 가드를 함께 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeTabViewModelTest {
    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `초기 성공 - 실물 UseCase 결과를 화면 상태로 매핑하고 이름을 캐시한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.server.enqueueRequest().completeSuccess(
                userName = "효기",
                isRecipientDesignated = true,
                questionContent = "오늘 가장 고마웠던 일은?",
            )

            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            assertEquals(
                successState(
                    userName = "효기",
                    isRecipientDesignated = true,
                    questionContent = "오늘 가장 고마웠던 일은?",
                ),
                viewModel.uiState.value,
            )
            assertEquals(listOf("효기"), fixture.profile.savedUserNames)
            assertEquals(1, fixture.server.userRepository.profileCalls)
            assertEquals(1, fixture.server.userRepository.receiverCalls)
            assertEquals(1, fixture.server.dailyQuestionRepository.getTodayCalls)
        }

    @Test
    fun `보조 조회 실패 - 일기 수와 오늘의 질문만 폴백하고 홈은 성공한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.server.enqueueRequest().completeWithAuxiliaryFailures(
                userName = "효기",
                isRecipientDesignated = false,
            )

            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            assertEquals(
                successState(
                    userName = "효기",
                    isRecipientDesignated = false,
                    questionContent = null,
                ),
                viewModel.uiState.value,
            )
            assertTrue(fixture.reporter.failures.isEmpty())
        }

    @Test
    fun `초기 필수 조회 실패 - 캐시 이름을 로딩에 쓰고 오류 상태와 리포팅으로 전환한다`() =
        runTest(dispatcher) {
            val fixture = Fixture().apply { profile.cachedUserName = "캐시 이름" }
            val request = fixture.server.enqueueRequest()
            val viewModel = fixture.viewModel()

            runCurrent()
            assertEquals(HomeTabUiState.Loading(cachedUserName = "캐시 이름"), viewModel.uiState.value)

            val failure = IllegalStateException("프로필 실패")
            request.failProfile(failure)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is HomeTabUiState.Error)
            val reportedFailure = (state as HomeTabUiState.Error).throwable
            assertEquals(failure::class, reportedFailure::class)
            assertEquals(failure.message, reportedFailure.message)
            assertEquals(
                "author_summary_load",
                fixture.reporter.failures
                    .single()
                    .attributes["home_stage"],
            )
        }

    @Test
    fun `자동 갱신 실패 - 보던 성공 상태를 유지하고 실패만 기록한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.server.enqueueRequest().completeSuccess(userName = "기존")
            val viewModel = fixture.viewModel()
            advanceUntilIdle()
            val existingState = viewModel.uiState.value as HomeTabUiState.Success

            val refresh = fixture.server.enqueueRequest()
            viewModel.refreshOnReturn()
            runCurrent()

            assertEquals(existingState, viewModel.uiState.value)

            refresh.failProfile(IllegalStateException("자동 갱신 실패"))
            advanceUntilIdle()

            assertEquals(existingState, viewModel.uiState.value)
            assertEquals(
                "author_summary_load",
                fixture.reporter.failures
                    .single()
                    .attributes["home_stage"],
            )
        }

    @Test
    fun `사용자 갱신 실패 - 진행 표시 후 오류 상태로 전환한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.server.enqueueRequest().completeSuccess(userName = "기존")
            val viewModel = fixture.viewModel()
            advanceUntilIdle()
            val existingState = viewModel.uiState.value as HomeTabUiState.Success

            val refresh = fixture.server.enqueueRequest()
            viewModel.loadHomeSummary(isRefresh = true)
            runCurrent()

            assertEquals(existingState.copy(isRefreshing = true), viewModel.uiState.value)

            val failure = IllegalStateException("사용자 갱신 실패")
            refresh.failProfile(failure)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is HomeTabUiState.Error)
            val reportedFailure = (state as HomeTabUiState.Error).throwable
            assertEquals(failure::class, reportedFailure::class)
            assertEquals(failure.message, reportedFailure.message)
        }

    @Test
    fun `사용자 갱신 - 매달린 자동 갱신을 취소하고 오래된 응답보다 우선한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.server.enqueueRequest().completeSuccess(userName = "최초")
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            val automaticRefresh = fixture.server.enqueueRequest()
            viewModel.refreshOnReturn()
            runCurrent()

            val userRefresh = fixture.server.enqueueRequest()
            viewModel.loadHomeSummary(isRefresh = true)
            runCurrent()

            userRefresh.completeSuccess(userName = "사용자 요청")
            advanceUntilIdle()
            val userState = successState(userName = "사용자 요청")
            assertEquals(userState, viewModel.uiState.value)

            automaticRefresh.completeSuccess(userName = "취소된 자동 갱신")
            runCurrent()

            assertEquals(userState, viewModel.uiState.value)
            assertEquals(3, fixture.server.userRepository.profileCalls)
            assertEquals(listOf("최초", "사용자 요청"), fixture.profile.savedUserNames)
            assertTrue(fixture.reporter.failures.isEmpty())
        }

    @Test
    fun `사용자 갱신 중 추가 사용자 요청 - 새 호출 없이 앞선 요청을 유지한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.server.enqueueRequest().completeSuccess(userName = "최초")
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            val firstRefresh = fixture.server.enqueueRequest()
            viewModel.loadHomeSummary(isRefresh = true)
            runCurrent()

            viewModel.loadHomeSummary(isRefresh = true)
            runCurrent()

            assertEquals(2, fixture.server.userRepository.profileCalls)
            assertEquals(2, fixture.server.userRepository.receiverCalls)

            firstRefresh.completeSuccess(userName = "첫 사용자 요청")
            advanceUntilIdle()

            assertEquals(successState(userName = "첫 사용자 요청"), viewModel.uiState.value)
        }
}

private class Fixture {
    val server = FakeHomeRepositories()
    val profile =
        FakeUserProfileCacheRepository.strict().apply {
            onGetCachedUserName = { cachedUserName }
            onSaveUserName = { name -> cachedUserName = name }
        }
    val reporter = RecordingErrorReporter()

    fun viewModel(): HomeTabViewModel =
        HomeTabViewModel(
            getHomeSummary =
                GetHomeSummaryUseCase(
                    // 같은 페이크가 두 좁은 계약을 다 구현한다 — UserRepository 가 둘을
                    // 상속하므로, 이 테스트가 쥔 완료 시점 제어가 그대로 유지된다 (#1742).
                    myProfileRepository = server.userRepository,
                    userReceiverRepository = server.userRepository,
                    dailyQuestionRepository = server.dailyQuestionRepository,
                    // 주간 기록 수는 이 테스트의 관심사가 아니다 — 실패로 고정해 보조 호출
                    // 실패가 홈 전체를 깨뜨리지 않는다는 계약도 함께 태운다 (#562).
                    getWeeklyRecordCount = GetWeeklyRecordCountUseCase(FailingWeeklyReportRepository),
                ),
            userProfileCacheRepository = profile,
            errorReporter = reporter,
        )
}

private class FakeHomeRepositories {
    private val profiles = ArrayDeque<CompletableDeferred<User>>()
    private val receivers = ArrayDeque<CompletableDeferred<List<Receiver>>>()

    val userRepository =
        FakeUserRepository.strict().apply {
            onGetReceivers = { receivers.takeNext("getReceivers").await() }
            onGetMyProfile = { profiles.takeNext("getMyProfile").await() }
        }

    /** 완료 시점을 테스트가 쥐고 있어야 병렬 조회의 경합 순서를 만들 수 있다. */
    private val questionResults = ArrayDeque<CompletableDeferred<Result<TodayDailyQuestion>>>()

    val dailyQuestionRepository =
        FakeDailyQuestionRepository.strict().apply {
            onGetToday = { questionResults.takeNext("DailyQuestionRepository.getToday").await() }
        }

    fun enqueueRequest(): PendingHomeRequest =
        PendingHomeRequest().also { request ->
            profiles.addLast(request.profile)
            receivers.addLast(request.receivers)
            questionResults.addLast(request.question)
        }
}

/** 병렬 조회를 한 요청 단위로 묶어 테스트가 완료 순서를 직접 정한다. */
private class PendingHomeRequest {
    val profile = CompletableDeferred<User>()
    val receivers = CompletableDeferred<List<Receiver>>()
    val question = CompletableDeferred<Result<TodayDailyQuestion>>()

    fun completeSuccess(
        userName: String,
        isRecipientDesignated: Boolean = false,
        questionContent: String = "오늘의 질문",
    ) {
        completeRequired(userName, isRecipientDesignated)
        question.complete(Result.success(todayQuestion(questionContent)))
    }

    fun completeWithAuxiliaryFailures(
        userName: String,
        isRecipientDesignated: Boolean,
    ) {
        completeRequired(userName, isRecipientDesignated)
        question.complete(Result.failure(IllegalStateException("오늘의 질문 조회 실패")))
    }

    fun failProfile(failure: Throwable) {
        profile.completeExceptionally(failure)
        receivers.complete(emptyList())
        question.complete(Result.success(todayQuestion("미사용")))
    }

    private fun completeRequired(
        userName: String,
        isRecipientDesignated: Boolean,
    ) {
        profile.complete(testUser(userName))
        receivers.complete(if (isRecipientDesignated) listOf(TEST_RECEIVER) else emptyList())
    }
}

private class RecordingErrorReporter : ErrorReporter {
    data class Failure(
        val throwable: Throwable,
        val attributes: Map<String, String>,
    )

    val failures = mutableListOf<Failure>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        failures += Failure(throwable, attributes)
    }
}

private fun <T> ArrayDeque<T>.takeNext(method: String): T =
    if (isEmpty()) {
        error("$method 응답이 준비되지 않음")
    } else {
        removeFirst()
    }

private fun successState(
    userName: String,
    isRecipientDesignated: Boolean = false,
    questionContent: String? = "오늘의 질문",
): HomeTabUiState.Success =
    HomeTabUiState.Success(
        userName = userName,
        isRecipientDesignated = isRecipientDesignated,
        todayQuestionContent = questionContent,
    )

private fun testUser(name: String): User =
    User(
        name = name,
        email = "user@example.com",
        phone = null,
        profileImageUrl = null,
    )

private fun todayQuestion(content: String): TodayDailyQuestion =
    TodayDailyQuestion(
        questionId = 1L,
        day = 1,
        content = content,
        isAnswered = false,
    )

private val TEST_RECEIVER =
    Receiver(
        receiverId = 1L,
        name = "수신자",
        relation = "가족",
        authCode = "auth-code",
    )

/**
 * 주간 기록 수는 이 테스트의 관심사가 아니다 — 실패로 고정해, 보조 호출 하나가 실패해도
 * 홈 전체가 깨지지 않는다는 계약도 함께 태운다 (#562).
 */
private object FailingWeeklyReportRepository : WeeklyReportRepository {
    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> = Result.failure(IllegalStateException("조회 안 함"))
}
