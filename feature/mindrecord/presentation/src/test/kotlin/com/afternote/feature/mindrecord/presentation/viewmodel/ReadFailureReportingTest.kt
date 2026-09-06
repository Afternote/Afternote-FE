package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 읽기 실패 계측의 **선별 기준** 가드 (#964).
 *
 * 이 모듈은 종전에 `Log.e` 도 `ErrorReporter` 도 0건이라, 실패가 UI 상태로만 흡수되고
 * 릴리즈에서 무슨 일이 있었는지 알 방법이 없었다. 쓰기 실패는 먼저 승격됐고 이 파일은
 * **읽기** 몫이다.
 *
 * ### 전부 올리지 않는다
 *
 * Crashlytics 는 non-fatal 을 최근 8건만 보관하고 초과분을 버린다. 목록 조회 실패를 전부
 * 올리면 네트워크가 한 번 끊길 때 잡음이 한도를 채워 **실제 장애를 밀어낸다.**
 *
 * 그래서 선을 하나 긋는다 — **사용자가 오류 화면을 마주한 실패만 올린다.** 재진입 갱신
 * (`refreshOnReturn`)은 실패해도 보고 있던 목록을 그대로 두므로 올리지 않는다. 화면 이탈이
 * 잦은 목록에서 그쪽이 잡음의 대부분이다.
 *
 * 이 파일이 지키는 것은 그 선이다. 「기록되는가」뿐 아니라 **「기록되지 않는가」**를 함께 본다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
// 상세 ViewModel 이 `SavedStateHandle.toRoute()` 로 route 를 복원하는데 그 경로가 Android
// `Bundle` 을 탄다 — 순수 JVM 으로는 `putLong ... not mocked` 로 죽는다.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReadFailureReportingTest {
    private val dispatcher = StandardTestDispatcher()

    // ViewModel 은 `viewModelScope`(Main) 에서 돈다 — 바꿔 놓지 않으면 코루틴이 아예
    // 실행되지 않아 「기록되지 않았다」 단언이 **아무 것도 안 돌아서** 통과한다.
    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `일기 목록이 오류 화면으로 바뀌면 기록한다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val viewModel = failingDiaryListViewModel(reporter)

            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            assertEquals(listOf("record_list_load"), reporter.stages)
        }

    /**
     * **이 테스트가 선을 지킨다.** 재진입 갱신 실패는 화면을 갈아치우지 않으므로 사용자에게
     * 보이지도 않는다 — 그것까지 올리면 한도가 잡음으로 찬다.
     *
     * `refreshOnReturn` 은 `loadedVersion == changeTracker.version` 이면 즉시 반환한다. 트래커를
     * 새로 만들어 넘기면 첫 조회 뒤 두 값이 같아져 **재조회가 아예 안 나가고**, 심어 둔 실패가
     * 호출조차 되지 않은 채 「기록 없음」이 통과한다 — 계측을 «전부 올리기» 로 되돌려도 초록인
     * 공허한 테스트가 된다 (#964 리뷰). 그래서 쓰기를 한 번 알리고, **갱신이 실제로 나갔는지**를
     * 조회 횟수로 함께 못박는다.
     */
    @Test
    fun `재진입 갱신이 실패해도 목록이 남아 있으면 기록하지 않는다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val diaryRepository = FakeDiaryRepository()
            val changeTracker = MindRecordChangeTracker()
            val viewModel =
                DiaryListViewModel(
                    repository = diaryRepository,
                    changeTracker = changeTracker,
                    errorReporter = reporter,
                )
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()
            // 첫 조회는 성공해 목록이 떠 있다.
            assertEquals(emptyList<String>(), reporter.stages)
            assertEquals(1, diaryRepository.listQueries.size)

            diaryRepository.onGetList = { _, _ -> Result.failure(IOException("갱신 실패")) }
            // 쓰기가 있었다고 알린다 — 없으면 버전 가드가 재조회를 건너뛴다.
            changeTracker.notifyChanged()
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals("재조회가 나가지 않아 실패 자체가 없었다", 2, diaryRepository.listQueries.size)
            assertTrue("갱신 실패가 화면을 갈아치웠다", viewModel.uiState.value is DiaryListUiState.Success)
            assertEquals("보고 있던 목록이 유지되는 실패까지 올라갔다", emptyList<String>(), reporter.stages)
        }

    @Test
    fun `데일리질문 목록이 오류 화면으로 바뀌면 기록한다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val viewModel =
                DailyQuestionListViewModel(
                    repository = FakeDailyQuestionRepository(onGetList = { _, _ -> Result.failure(IOException("조회 실패")) }),
                    changeTracker = MindRecordChangeTracker(),
                    errorReporter = reporter,
                )

            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            assertEquals(listOf("record_list_load"), reporter.stages)
        }

    /**
     * 작성 화면이 여는 조회다. 여기 실패하면 화면이 오류 문구만 남고 **쓸 수가 없다** —
     * 목록 실패와 달리 「보고 있던 것」이 없다 (#964).
     */
    @Test
    fun `작성 화면이 오늘 질문을 못 받으면 기록한다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val repository =
                FakeDailyQuestionRepository().apply {
                    onGetToday = { Result.failure(IOException("질문 조회 실패")) }
                }
            DailyQuestionWriteViewModel(
                savedStateHandle = SavedStateHandle(emptyMap()),
                repository = repository,
                photoUploadRepository = FakePhotoUploadRepository.strict(),
                draftLoader = LoadMindRecordDraftsUseCase(FakeDiaryRepository(), repository),
                errorReporter = reporter,
            )
            advanceUntilIdle()

            assertEquals(listOf("daily_question_load"), reporter.stages)
        }

    /**
     * 추억 공간은 **부분 실패를 삼킨다** — 카드가 한 장이라도 차면 그대로 보여 준다.
     * 그 선을 지키는 음성 단언이 없으면 「전부 올리기」로 되돌려도 초록이다 (#964 리뷰).
     */
    @Test
    fun `추억 공간은 한쪽만 실패하면 기록하지 않는다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            MemorySpaceViewModel(
                diaryRepository = FakeDiaryRepository(onGetList = { _, _ -> Result.failure(IOException("일기 실패")) }),
                dailyQuestionRepository = FakeDailyQuestionRepository(initialAnswers = listOf(answeredQuestion())),
                errorReporter = reporter,
            )
            advanceUntilIdle()

            assertEquals("부분 실패까지 올라갔다", emptyList<String>(), reporter.stages)
        }

    /** 되돌릴 수 없는 동작이라 화면만 알리고 끝내면 나중에 물을 곳이 없다. */
    @Test
    fun `기록 삭제가 실패하면 기록한다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val diaryRepository = FakeDiaryRepository(onDelete = { Result.failure(IOException("삭제 실패")) })
            val viewModel =
                DiaryListViewModel(
                    repository = diaryRepository,
                    changeTracker = MindRecordChangeTracker(),
                    errorReporter = reporter,
                )
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.delete(1L)
            advanceUntilIdle()

            assertEquals(listOf("record_delete"), reporter.stages)
        }

    /**
     * 데일리질문 쪽도 같은 계약이다. 종전에는 이 계측을 지워도 초록이었다 —
     * `MindRecordFailureRecoveryTest` 가 같은 경로를 돌리지만 reporter 단언이 없다 (#964 리뷰).
     */
    @Test
    fun `데일리질문 삭제가 실패하면 기록한다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val viewModel =
                DailyQuestionListViewModel(
                    repository = FakeDailyQuestionRepository(onDelete = { Result.failure(IOException("삭제 실패")) }),
                    changeTracker = MindRecordChangeTracker(),
                    errorReporter = reporter,
                )
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.delete(1L)
            advanceUntilIdle()

            assertEquals(listOf("record_delete"), reporter.stages)
        }

    /** 「내가 쓴 기록이 안 열린다」 — 재현 조건(그 계정의 그 기록)이 우리 손에 없다. */
    @Test
    fun `기록 상세 조회가 실패하면 기록한다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            RecordDetailViewModel(
                savedStateHandle = detailRoute(isDiary = true),
                diaryRepository = FakeDiaryRepository(onGetList = { _, _ -> Result.failure(IOException("상세 실패")) }),
                dailyQuestionRepository = FakeDailyQuestionRepository(),
                errorReporter = reporter,
            )
            advanceUntilIdle()

            assertEquals(listOf("record_detail_load"), reporter.stages)
        }

    /**
     * 추억 공간은 **부분 실패를 삼킨다** — 카드가 한 장이라도 차면 그대로 보여 준다.
     * 그러니 여기 올라오는 것은 **합친 결과가 비었고 실패 출처가 하나라도 있을 때**다.
     * 출처는 둘이 아니라 넷이다 — 일기는 최근 3개월을 달마다 따로 조회하고(`DIARY_MONTH_WINDOW`)
     * 데일리질문이 하나다. 그래서 일기 0건(신규 사용자)에 질문 조회 하나만 실패해도 올라온다.
     */
    @Test
    fun `추억 공간이 통째로 실패하면 기록한다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            MemorySpaceViewModel(
                diaryRepository = FakeDiaryRepository(onGetList = { _, _ -> Result.failure(IOException("일기 실패")) }),
                dailyQuestionRepository = FakeDailyQuestionRepository(onGetList = { _, _ -> Result.failure(IOException("질문 실패")) }),
                errorReporter = reporter,
            )
            advanceUntilIdle()

            assertEquals(listOf("memory_space_load"), reporter.stages)
        }

    /** 추억 공간이 「한 장은 찼다」로 볼 수 있는 최소 재료. */
    private fun answeredQuestion() =
        DailyQuestion(
            dailyQuestionId = 1L,
            title = "질문",
            content = "답변",
            createdAt = "2026-08-23T10:00:00",
            isDraft = false,
        )

    /** 조회가 처음부터 실패하는 일기 목록 VM. 성공 경로는 각 테스트가 직접 만든다. */
    private fun failingDiaryListViewModel(reporter: RecordingErrorReporter): DiaryListViewModel =
        DiaryListViewModel(
            repository = FakeDiaryRepository(onGetList = { _, _ -> Result.failure(IOException("조회 실패")) }),
            changeTracker = MindRecordChangeTracker(),
            errorReporter = reporter,
        )

    private fun detailRoute(isDiary: Boolean): SavedStateHandle =
        SavedStateHandle(
            mapOf(
                "recordId" to 1L,
                "isDiary" to isDiary,
                "yearMonth" to "2026-09",
            ),
        )
}
