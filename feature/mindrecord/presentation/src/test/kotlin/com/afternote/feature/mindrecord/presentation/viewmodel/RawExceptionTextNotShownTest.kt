package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.domain.testing.FakeWeeklyReportRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import com.afternote.feature.mindrecord.presentation.usecase.ObserveWeeklyReportUseCase
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
 * 예외 원문이 화면 상태에 실리지 않는지 잠근다 (#1882).
 *
 * 이 저장소는 **서버 오류 원문을 화면에 내지 않는다** — 로그·텔레메트리로만 보낸다
 * (#1339 에서 수신자 흐름에 적용). 마음의 기록 두 자리는 그 선 밖에 있었다:
 * `UiText.DynamicOrResource(value = e.message, …)` 는 `asString()` 이
 * `value ?: stringResource(fallback)` 이라 **`e.message` 가 있으면 fallback 을 아예 쓰지
 * 않는다.** 직렬화 예외 문구·영문 스택 용어가 그대로 사용자에게 갔다.
 *
 * 가정이 아니라 한 번 일어났던 일이다 — `EmotionAnalysisContractTest` 의 주석이
 * 「`MissingFieldException` 이 나 … `DynamicOrResource` 가 `e.message` 를 우선해 영문 예외
 * 원문까지 노출됐다」로 그 사고를 기록하고 있다. 그때는 DTO 필드를 옵셔널로 내려 그 예외만
 * 막았고, 노출 기전은 살아 있었다.
 *
 * ### 무엇을 단언하는가
 *
 * **타입**을 본다 — 상태에 실린 `UiText` 가 `UiText.Resource` 여야 한다. 문구를 비교하면
 * 문자열이 바뀔 때마다 같이 고쳐야 하고, 정작 지키려는 것(원문이 실리는가)은 못 본다.
 * `DynamicOrResource` 로 되돌리면 이 단언이 바로 빨개진다.
 *
 * 원문이 **유실되지도 않아야 한다** — 같은 실패가 `ErrorReporter` 에 단계 키와 함께 남는지
 * 함께 본다. 화면에서 걷어내고 계측에도 안 남기면 릴리즈에서 아무 단서가 없다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RawExceptionTextNotShownTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `이어쓰기 조회 실패가 예외 문구 대신 안내 문자열을 싣는다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val repository =
                FakeDailyQuestionRepository().apply {
                    onGetList = { _, _ -> Result.failure(IOException("Unresolved field 'daily-question' at offset 42")) }
                }
            val viewModel =
                DailyQuestionWriteViewModel(
                    // 이어쓰기 진입 — 이 경로가 조회에 실패하는 자리다.
                    savedStateHandle = SavedStateHandle(mapOf("answerId" to 7L, "isDraft" to true)),
                    repository = repository,
                    photoUploadRepository = FakePhotoUploadRepository.strict(),
                    draftLoader = LoadMindRecordDraftsUseCase(FakeDiaryRepository(), repository),
                    errorReporter = reporter,
                )
            advanceUntilIdle()

            val shown = viewModel.uiState.value.questionLoadError
            assertTrue(
                "예외 원문이 화면 상태에 실렸다: $shown",
                shown is UiText.Resource,
            )
            assertEquals("원문은 계측으로 남아야 한다", listOf("daily_question_load"), reporter.stages)
        }

    @Test
    fun `주간 리포트 조회 실패가 예외 문구 대신 안내 문자열을 싣는다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val repository =
                FakeWeeklyReportRepository().apply {
                    results.addLast(Result.failure(IOException("HTTP 500 Internal Server Error: gemini quota exceeded")))
                }
            val viewModel =
                WeeklyReportViewModel(
                    ObserveWeeklyReportUseCase(repository, FakeUserRepository()),
                    MindRecordChangeTracker(),
                    reporter,
                )
            // uiState 가 WhileSubscribed 라 구독자가 없으면 Loading 에 머문다.
            backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("오류 상태여야 한다: $state", state is WeeklyReportUiState.Error)
            state as WeeklyReportUiState.Error
            assertTrue(
                "예외 원문이 화면 상태에 실렸다: ${state.message}",
                state.message is UiText.Resource,
            )
            assertEquals("원문은 계측으로 남아야 한다", listOf("weekly_report_load"), reporter.stages)
        }
}
