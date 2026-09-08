package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.lang.reflect.Proxy
import java.time.LocalDate

/**
 * 사용자가 고른 기록일이 실제로 요청에 실리는지 (#1008).
 *
 * 종전에는 날짜 선택이 **표시 전용**이었다 — 서버 스키마에 `date` 가 없어 고른 값이 요청에
 * 실리지 않았고, #1121 이 「고를 수 있지만 반영되지 않는」 상태를 없애려 피커를 걷었다.
 * 서버가 2026-08-29 부터 생성·수정 양쪽에서 `date` 를 받으므로(`Afternote-BE#244`, PR #262)
 * 피커를 되돌렸고, 이 가드가 그 배선을 잠근다.
 *
 * 이 파일은 `DiaryWriteDateDisplayTest`(「날짜를 바꾸는 공개 창구가 없다」)를 대체한다 —
 * 그 단언은 이제 정반대다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
// 수정 진입 시나리오가 `SavedStateHandle.toRoute()` 로 Bundle 을 탄다 — 순수 JVM 에서는
// `BaseBundle.putLong` 이 not-mocked 로 죽는다.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiaryRecordDateWireTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `신규 작성은 고른 날짜를 그대로 싣는다`() =
        runTest(dispatcher) {
            val repository = FakeDiaryRepository()
            val viewModel = diaryViewModel(repository, SavedStateHandle(emptyMap()))
            advanceUntilIdle()

            val chosen = LocalDate.now().minusDays(3)
            viewModel.onDateSelected(chosen)
            fillRequiredFields(viewModel)
            viewModel.submit()
            advanceUntilIdle()

            assertEquals(chosen, repository.createdPayloads.single().date)
        }

    @Test
    fun `고르지 않으면 오늘이 실린다`() =
        runTest(dispatcher) {
            val repository = FakeDiaryRepository()
            val viewModel = diaryViewModel(repository, SavedStateHandle(emptyMap()))
            advanceUntilIdle()

            fillRequiredFields(viewModel)
            viewModel.submit()
            advanceUntilIdle()

            assertEquals(LocalDate.now(), repository.createdPayloads.single().date)
        }

    @Test
    fun `미래 날짜는 상태에 들어가지 않는다`() =
        runTest(dispatcher) {
            // 서버가 400(code 2101)으로 거절하는 값이다. 상태에 넣으면 사용자는 저장을
            // 눌러 봐야 실패를 안다 — 고르는 순간 사유와 함께 막는다.
            val repository = FakeDiaryRepository()
            val viewModel = diaryViewModel(repository, SavedStateHandle(emptyMap()))
            advanceUntilIdle()

            val today = viewModel.uiState.value.date
            viewModel.onDateSelected(LocalDate.now().plusDays(1))

            assertEquals(today, viewModel.uiState.value.date)
            assertEquals(false, viewModel.uiState.value.isDateChosen)
            assertEquals(true, viewModel.uiState.value.dateError != null)
        }

    @Test
    fun `프리필이 날짜를 못 주면 수정은 기록일을 싣지 않는다`() =
        runTest(dispatcher) {
            // 여기서 «오늘» 을 실어 보내면 기존 기록일이 조용히 오늘로 옮겨진다. 서버는
            // 키를 생략하면 기존 값을 유지하므로 생략이 맞다.
            val repository = FakeDiaryRepository()
            repository.onGetList = { _, _ -> Result.failure(IllegalStateException("offline")) }
            val viewModel = diaryViewModel(repository, editRoute())
            advanceUntilIdle()

            fillRequiredFields(viewModel)
            viewModel.submit()
            advanceUntilIdle()

            assertNull(
                repository.updatedPayloads
                    .single()
                    .second.date,
            )
        }

    @Test
    fun `프리필이 준 날짜는 수정 요청에 그대로 남는다`() =
        runTest(dispatcher) {
            val repository = FakeDiaryRepository(initialDiaries = listOf(existingDiary(date = "2026-07-11")))
            val viewModel = diaryViewModel(repository, editRoute())
            advanceUntilIdle()

            viewModel.submit()
            advanceUntilIdle()

            assertEquals(
                LocalDate.of(2026, 7, 11),
                repository.updatedPayloads
                    .single()
                    .second.date,
            )
        }

    private fun fillRequiredFields(viewModel: DiaryWriteViewModel) {
        viewModel.onTitleChanged("제목")
        viewModel.onContentChanged("<p>본문</p>")
        viewModel.onMoodSelected(TodayMood.HAPPY)
    }

    private fun editRoute() =
        SavedStateHandle(
            mapOf("recordId" to EXISTING_ID, "yearMonth" to "2026-07", "isDraft" to false),
        )

    private fun existingDiary(date: String) =
        Diary(
            diaryId = EXISTING_ID,
            title = "기존 제목",
            content = "<p>기존 본문</p>",
            date = date,
            createdAt = "2026-07-11T09:00:00",
            todayMood = TodayMood.SOSO,
            isDraft = false,
        )

    private fun diaryViewModel(
        repository: FakeDiaryRepository,
        savedStateHandle: SavedStateHandle,
    ): DiaryWriteViewModel =
        DiaryWriteViewModel(
            savedStateHandle = savedStateHandle,
            repository = repository,
            // 이 시나리오는 업로드를 타지 않는다 — 부르면 그 자체가 결함이라 실패로 둔다.
            photoUploadRepository = PhotoUploadRepository { _, _ -> Result.failure(IOException("unused")) },
            userRepository = emptyReceiverRepository(),
            draftLoader = LoadMindRecordDraftsUseCase(repository, FakeDailyQuestionRepository()),
            errorReporter = RecordingErrorReporter(),
        )

    /** UserRepository 는 표면이 넓다 — 이 시나리오가 타는 호출만 답한다. */
    private fun emptyReceiverRepository(): UserReceiverRepository =
        Proxy.newProxyInstance(
            UserReceiverRepository::class.java.classLoader,
            arrayOf(UserReceiverRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getReceivers" -> emptyList<Any>()
                "getReceiverListFlow" -> flowOf(emptyList<Any>())
                else -> error("Unexpected call: ${method.name}")
            }
        } as UserReceiverRepository

    private companion object {
        const val EXISTING_ID = 4242L
    }
}
