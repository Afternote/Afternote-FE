package com.afternote.feature.home.presentation.receiver

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.home.presentation.receiver.model.ReceiverDownloadState
import com.afternote.feature.home.presentation.receiver.model.ReceiverHomeUiState
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.testing.FakeMindRecordReceiverRepository
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.testing.FakeReceiverTimeLetterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary as DomainMindRecordSummary

/** 수신자 홈이 각 기능의 실제 domain repository 결과를 구분해 합치는 계약 회귀 가드 (#610). */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverHomeViewModelTest {
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
    fun `성공 - 세 기록 repository의 실제 합계와 마음의 기록 하위 합계를 표시`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.receiver.onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 5)) }
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 2, diaryCount = 3))
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 4) }
            fixture.receiver.onLoadSenderMessage = {
                Result.success(
                    SenderMessageInfo(
                        senderName = "서연",
                        message = "잘 지내길 바랄게.",
                        createdAt = "2026.08.22",
                    ),
                )
            }

            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ReceiverHomeUiState.Success
            assertEquals(5, state.afternoteTotalCount)
            assertEquals(4, state.timeLetterTotalCount)
            assertEquals(5, state.mindRecord?.totalCount)
            assertEquals(2, state.mindRecord?.dailyQuestionCount)
            assertEquals(3, state.mindRecord?.diaryCount)
            assertEquals("서연", state.senderName)
            assertTrue(fixture.reporter.failures.isEmpty())
        }

    @Test
    fun `정상 빈 목록 - 조회 실패 null과 구분되는 0을 유지`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.receiver.onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 0)) }
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 0, diaryCount = 0))
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 0) }

            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ReceiverHomeUiState.Success
            assertEquals(0, state.afternoteTotalCount)
            assertEquals(0, state.timeLetterTotalCount)
            assertEquals(0, state.mindRecord?.totalCount)
            assertTrue(fixture.reporter.failures.isEmpty())
        }

    @Test
    fun `부분 실패 - 실패 섹션은 null로 구분하고 성공 섹션과 실패 출처를 보존`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            val afternoteFailure = IllegalStateException("애프터노트 실패")
            fixture.receiver.onGetReceivedAfterNotes = { Result.failure(afternoteFailure) }
            fixture.mindRecord.result = Result.failure(IllegalStateException("마음의 기록 실패"))
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 2) }

            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ReceiverHomeUiState.Success
            assertNull(state.afternoteTotalCount)
            assertNull(state.mindRecord)
            assertEquals(2, state.timeLetterTotalCount)

            val reported = fixture.reporter.failures.single()
            assertEquals(afternoteFailure.javaClass.name, reported.throwable.message)
            assertEquals(afternoteFailure.javaClass.name, reported.attributes["error_type"])
            assertEquals("receiver_home_partial_load", reported.attributes["receiver_stage"])
            assertEquals("afternotes,mind_records", reported.attributes["receiver_failed_sources"])
        }

    @Test
    fun `전체 실패 - 성공 화면의 가짜 0 대신 Error와 전체 실패 단계 기록`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            val firstFailure = IllegalStateException("애프터노트 실패")
            fixture.receiver.onGetReceivedAfterNotes = { Result.failure(firstFailure) }
            fixture.mindRecord.result = Result.failure(IllegalStateException("마음의 기록 실패"))
            fixture.timeLetter.onGetReceivedTimeLetters = { throw IllegalStateException("타임레터 실패") }
            fixture.receiver.onLoadSenderMessage = { Result.failure(IllegalStateException("한 마디 실패")) }

            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ReceiverHomeUiState.Error)
            assertSame(firstFailure, (state as ReceiverHomeUiState.Error).throwable)

            val reported = fixture.reporter.failures.single()
            assertEquals(firstFailure.javaClass.name, reported.throwable.message)
            assertEquals(firstFailure.javaClass.name, reported.attributes["error_type"])
            assertEquals("receiver_home_load", reported.attributes["receiver_stage"])
            assertNull(reported.attributes["receiver_failed_sources"])
        }

    // region 재진입 갱신 (#701)

    @Test
    fun `첫 진입 resume 은 재조회를 트리거하지 않는다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.receiver.onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 5)) }
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 1, diaryCount = 1))
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 1) }
            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            // 첫 진입 화면의 ON_RESUME (init 로드는 이미 종료됨) — 재조회가 걸리면 안 된다.
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals(1, fixture.receiver.getReceivedAfterNotesCalls)
            assertTrue(viewModel.uiState.value is ReceiverHomeUiState.Success)
        }

    @Test
    fun `init 로드가 즉시 끝나도 첫 resume 은 재조회하지 않는다`() =
        runTest(dispatcher) {
            // `viewModelScope` 는 `Dispatchers.Main` 을 탄다 — 창을 만들려면 Main 자체가
            // 즉시 실행돼야 하므로 이 테스트에서만 Unconfined 로 바꾼다(tearDown 이 되돌린다).
            Dispatchers.setMain(UnconfinedTestDispatcher(dispatcher.scheduler))

            // **가드가 무엇으로 서 있는지 가르는 테스트다** (#1374 리뷰).
            //
            // 다른 테스트는 `advanceUntilIdle()` 로 init 로드를 끝낸 뒤 resume 을 부른다. 그러면
            // `loadJob` 이 이미 완료라 「진행 중이면 건너뛴다」 가드로도 통과할 수 있다.
            //
            // 실제 화면의 창은 그것보다 좁다 — 저장소가 즉시 응답하면 init 로드가 **중단 없이**
            // 끝나고, 그 직후 첫 ON_RESUME 이 도착한다. 그 순간 `loadJob` 은 이미 비활성이므로
            // **`isFirstResume` 만이 두 번째 요청을 막는다.** Unconfined 로 그 창을 그대로 만든다.
            val fixture = Fixture()
            fixture.receiver.onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 5)) }
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 1, diaryCount = 1))
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 1) }

            val viewModel = fixture.viewModel()
            // init 로드가 여기까지 이미 끝나 있다 — advanceUntilIdle 을 부르지 않는다.
            assertEquals(1, fixture.receiver.getReceivedAfterNotesCalls)

            viewModel.refreshOnReturn()

            assertEquals("첫 resume 이 두 번째 요청을 만들었다", 1, fixture.receiver.getReceivedAfterNotesCalls)
            assertTrue(viewModel.uiState.value is ReceiverHomeUiState.Success)
        }

    @Test
    fun `refreshOnReturn - 진행 중인 로드와 겹치면 건너뛴다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.receiver.onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 5)) }
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 1, diaryCount = 1))
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 1) }

            val viewModel = fixture.viewModel()
            // init 로드가 아직 도는 중 — 첫 resume(스킵) 뒤 또 한 번 resume 이 와도 중복이 없어야 한다.
            viewModel.refreshOnReturn()
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals(1, fixture.receiver.getReceivedAfterNotesCalls)
            assertTrue(viewModel.uiState.value is ReceiverHomeUiState.Success)
        }

    @Test
    fun `refreshOnReturn - 복귀 시 로딩 없이 새 합계로 갱신하고 내려받기 상태를 유지한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.receiver.onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 5)) }
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 1, diaryCount = 1))
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 4) }
            val viewModel = fixture.viewModel()
            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            advanceUntilIdle()
            viewModel.onEvent(ReceiverHomeEvent.RequestDownload)
            fixture.receiver.onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 6)) }

            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME
            // 로딩을 방출하지 않는다 — 갱신이 도는 동안에도 기존 화면을 유지한다.
            assertEquals(5, (viewModel.uiState.value as ReceiverHomeUiState.Success).afternoteTotalCount)
            advanceUntilIdle()

            val refreshed = viewModel.uiState.value as ReceiverHomeUiState.Success
            assertEquals(6, refreshed.afternoteTotalCount)
            // 갱신이 화면을 교체해도 진행 중인 내려받기 다이얼로그 상태는 잃지 않는다.
            assertEquals(ReceiverDownloadState.Confirming, refreshed.download)
        }

    @Test
    fun `refreshOnReturn - 전체 실패는 보고 있던 화면을 유지하고 실패는 기록한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.receiver.onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 5)) }
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 1, diaryCount = 1))
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 4) }
            val viewModel = fixture.viewModel()
            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            advanceUntilIdle()

            fixture.receiver.onGetReceivedAfterNotes = { Result.failure(IllegalStateException("애프터노트 실패")) }
            fixture.mindRecord.result = Result.failure(IllegalStateException("마음의 기록 실패"))
            fixture.timeLetter.onGetReceivedTimeLetters = { throw IllegalStateException("타임레터 실패") }
            fixture.receiver.onLoadSenderMessage = { Result.failure(IllegalStateException("한 마디 실패")) }
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME
            advanceUntilIdle()

            // 잘 보고 있던 홈이 에러 화면으로 대체되지 않는다.
            val state = viewModel.uiState.value as ReceiverHomeUiState.Success
            assertEquals(5, state.afternoteTotalCount)
            // 화면에 안 보이는 실패인 만큼 콘솔 기록은 남긴다.
            assertEquals(
                "receiver_home_load",
                fixture.reporter.failures
                    .single()
                    .attributes["receiver_stage"],
            )
        }

    @Test
    fun `refreshOnReturn - 부분 실패도 완결된 기존 화면을 유지한다`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            fixture.receiver.onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 5)) }
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 1, diaryCount = 1))
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 4) }
            val viewModel = fixture.viewModel()
            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            advanceUntilIdle()

            // 한 소스만 실패, 나머지는 새 값으로 성공 — 실패 섹션을 null 로 꺼뜨린 부분 화면 대신
            // 완결된 기존 화면을 유지한다.
            fixture.receiver.onGetReceivedAfterNotes = { Result.failure(IllegalStateException("애프터노트 실패")) }
            fixture.timeLetter.onGetReceivedTimeLetters = { timeLetters(totalCount = 9) }
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME
            advanceUntilIdle()

            val state = viewModel.uiState.value as ReceiverHomeUiState.Success
            assertEquals(5, state.afternoteTotalCount)
            assertEquals(4, state.timeLetterTotalCount)
            assertEquals(
                "receiver_home_partial_load",
                fixture.reporter.failures
                    .single()
                    .attributes["receiver_stage"],
            )
        }

    // endregion
}

private class Fixture {
    val receiver =
        FakeReceiverRepository.strict().apply {
            onGetReceivedAfterNotes = { Result.success(afterNotes(totalCount = 0)) }
            onLoadSenderMessage = { Result.success(null) }
        }
    val mindRecord = FakeMindRecordReceiverRepository()
    val timeLetter = FakeReceiverTimeLetterRepository.strict()
    val reporter = RecordingErrorReporter()

    fun viewModel(): ReceiverHomeViewModel =
        ReceiverHomeViewModel(
            receiverRepository = receiver,
            mindRecordReceiverRepository = mindRecord,
            receiverTimeLetterRepository = timeLetter,
            errorReporter = reporter,
        )
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

private fun afterNotes(totalCount: Int): AfterNotesListResult = AfterNotesListResult(items = emptyList(), totalCount = totalCount)

private fun mindRecords(
    dailyQuestionCount: Int,
    diaryCount: Int,
): ReceiverMindRecords =
    ReceiverMindRecords(
        dailyQuestions = List(dailyQuestionCount) { mindRecord(id = it.toLong(), type = MindRecordType.DAILY_QUESTION) },
        diaries = List(diaryCount) { mindRecord(id = (it + 100).toLong(), type = MindRecordType.DIARY) },
    )

private fun mindRecord(
    id: Long,
    type: MindRecordType,
): DomainMindRecordSummary =
    DomainMindRecordSummary(
        id = id,
        type = type,
        title = "제목",
        content = "내용",
        recordDate = "2026-08-22",
        isDraft = false,
        createdAt = "2026.08.22 금요일",
    )

private fun timeLetters(totalCount: Int): ReceivedTimeLetterList =
    ReceivedTimeLetterList(timeLetters = emptyList(), totalCount = totalCount)
