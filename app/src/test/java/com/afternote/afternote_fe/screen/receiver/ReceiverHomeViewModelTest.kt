package com.afternote.afternote_fe.screen.receiver

import com.afternote.afternote_fe.screen.receiver.model.ReceiverHomeUiState
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
            fixture.receiver.afterNotes = Result.success(afterNotes(totalCount = 5))
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 2, diaryCount = 3))
            fixture.timeLetter.result = Result.success(timeLetters(totalCount = 4))
            fixture.receiver.message =
                Result.success(
                    SenderMessageInfo(
                        senderName = "서연",
                        message = "잘 지내길 바랄게.",
                        createdAt = "2026.08.22",
                    ),
                )

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
            fixture.receiver.afterNotes = Result.success(afterNotes(totalCount = 0))
            fixture.mindRecord.result = Result.success(mindRecords(dailyQuestionCount = 0, diaryCount = 0))
            fixture.timeLetter.result = Result.success(timeLetters(totalCount = 0))

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
            fixture.receiver.afterNotes = Result.failure(afternoteFailure)
            fixture.mindRecord.result = Result.failure(IllegalStateException("마음의 기록 실패"))
            fixture.timeLetter.result = Result.success(timeLetters(totalCount = 2))

            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ReceiverHomeUiState.Success
            assertNull(state.afternoteTotalCount)
            assertNull(state.mindRecord)
            assertEquals(2, state.timeLetterTotalCount)

            val reported = fixture.reporter.failures.single()
            assertEquals(afternoteFailure.javaClass.name, reported.throwable.message)
            assertEquals(afternoteFailure.javaClass.name, reported.attributes["error_type"])
            assertEquals("receiver_home_partial_load", reported.attributes["home_stage"])
            assertEquals("afternotes,mind_records", reported.attributes["home_failed_sources"])
        }

    @Test
    fun `전체 실패 - 성공 화면의 가짜 0 대신 Error와 전체 실패 단계 기록`() =
        runTest(dispatcher) {
            val fixture = Fixture()
            val firstFailure = IllegalStateException("애프터노트 실패")
            fixture.receiver.afterNotes = Result.failure(firstFailure)
            fixture.mindRecord.result = Result.failure(IllegalStateException("마음의 기록 실패"))
            fixture.timeLetter.result = Result.failure(IllegalStateException("타임레터 실패"))
            fixture.receiver.message = Result.failure(IllegalStateException("한 마디 실패"))

            val viewModel = fixture.viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ReceiverHomeUiState.Error)
            assertSame(firstFailure, (state as ReceiverHomeUiState.Error).throwable)

            val reported = fixture.reporter.failures.single()
            assertEquals(firstFailure.javaClass.name, reported.throwable.message)
            assertEquals(firstFailure.javaClass.name, reported.attributes["error_type"])
            assertEquals("receiver_home_load", reported.attributes["home_stage"])
            assertNull(reported.attributes["home_failed_sources"])
        }
}

private class Fixture {
    val receiver = FakeReceiverRepository()
    val mindRecord = FakeMindRecordReceiverRepository()
    val timeLetter = FakeReceiverTimeLetterRepository()
    val reporter = RecordingErrorReporter()

    fun viewModel(): ReceiverHomeViewModel =
        ReceiverHomeViewModel(
            receiverRepository = receiver,
            mindRecordReceiverRepository = mindRecord,
            receiverTimeLetterRepository = timeLetter,
            errorReporter = reporter,
        )
}

private class FakeReceiverRepository : ReceiverRepository {
    var afterNotes: Result<AfterNotesListResult> = Result.success(afterNotes(totalCount = 0))
    var message: Result<SenderMessageInfo?> = Result.success(null)

    override val authCodeFlow: Flow<String?> = flowOf(null)

    override suspend fun currentAuthCode(): String? = unexpected("currentAuthCode")

    override suspend fun saveAuthCode(code: String) = unexpected("saveAuthCode")

    override suspend fun clearAuthCode() = unexpected("clearAuthCode")

    override fun getPagedReceivedAfternotes() = unexpected("getPagedReceivedAfternotes")

    override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> = afterNotes

    override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> =
        unexpected("getReceivedAfternoteDetail")

    override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> = unexpected("downloadReceivedExport")

    override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> = unexpected("saveReceivedExportToFile")

    override suspend fun loadMindRecordsCount(): Result<LoadCountResult> = unexpected("legacy loadMindRecordsCount")

    override suspend fun loadTimeLettersCount(): Result<LoadCountResult> = unexpected("legacy loadTimeLettersCount")

    override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> = message
}

private class FakeMindRecordReceiverRepository : MindRecordReceiverRepository {
    var result: Result<ReceiverMindRecords> = Result.success(mindRecords(0, 0))

    override suspend fun getAll(): Result<ReceiverMindRecords> = result
}

private class FakeReceiverTimeLetterRepository : ReceiverTimeLetterRepository {
    var result: Result<ReceivedTimeLetterList> = Result.success(timeLetters(0))

    override suspend fun getReceivedTimeLetters(): ReceivedTimeLetterList = result.getOrThrow()

    override suspend fun getReceivedTimeLetterDetail(timeLetterReceiverId: Long): ReceivedTimeLetter =
        unexpected("getReceivedTimeLetterDetail")
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

private fun unexpected(method: String): Nothing = error("$method 는 이 테스트에서 호출되면 안 됨")
