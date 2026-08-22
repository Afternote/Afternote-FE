package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.paging.PagingData
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItem
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordBox
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceivedRecordsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `저장된 접근 코드 없음 - 이전 목록을 제거하고 서버는 호출하지 않음`() {
        val registry = SenderRegistry()
        registry.replaceRecordBoxes(listOf(recordBoxEntry(recordBoxId = 1L)))
        val repository = FakeReceiverRepository(initialAuthCode = null)

        val viewModel = ReceivedRecordsViewModel(registry, repository, RecordingErrorReporter())

        assertTrue(
            viewModel.uiState.value.senders
                .isEmpty(),
        )
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.hasLoadError)
        assertEquals(0, repository.recordBoxesCallCount)
    }

    @Test
    fun `접근 코드 있음 - 이전 받은 기록함을 조회 결과로 완전히 교체`() {
        val registry = SenderRegistry()
        registry.replaceRecordBoxes(listOf(recordBoxEntry(recordBoxId = 1L)))
        val receivedRecordBox = recordBox(recordBoxId = 18L)
        val repository =
            FakeReceiverRepository(
                initialAuthCode = "master-key",
                initialResult = Result.success(listOf(receivedRecordBox)),
            )

        val viewModel = ReceivedRecordsViewModel(registry, repository, RecordingErrorReporter())

        val senders = viewModel.uiState.value.senders
        val entry = senders.single()
        assertEquals(listOf(18L), senders.map(ReceivedRecordItem::recordBoxId))
        assertEquals("김혜성", entry.senderName)
        assertEquals("record-key-18", entry.accessCode)
        assertEquals(ReceivedRecordStatus.Stored, entry.recordStatus)
        assertEquals(ReceivedRecordViewStatus.Viewable, entry.viewStatus)
        assertEquals(
            "2026-07-30T04:25:42",
            (entry.verification as ReceivedRecordVerification.Approved).approvedAt,
        )
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.hasLoadError)
        assertEquals(1, repository.recordBoxesCallCount)
    }

    @Test
    fun `목록 실패 후 재시도 - 오류를 기록하고 성공 시 받은 기록함으로 복구`() {
        val registry = SenderRegistry()
        val repository =
            FakeReceiverRepository(
                initialAuthCode = "master-key",
                initialResult = Result.failure(IllegalStateException("secret")),
            )
        val reporter = RecordingErrorReporter()
        val viewModel = ReceivedRecordsViewModel(registry, repository, reporter)

        assertTrue(viewModel.uiState.value.hasLoadError)
        assertEquals("received_record_boxes_load", reporter.attributes.single()["afternote_stage"])

        repository.recordBoxesResult = Result.success(listOf(recordBox(recordBoxId = 20L)))
        viewModel.retry()

        assertEquals(
            listOf(20L),
            viewModel.uiState.value.senders
                .map(ReceivedRecordItem::recordBoxId),
        )
        assertFalse(viewModel.uiState.value.hasLoadError)
        assertEquals(2, repository.recordBoxesCallCount)
    }

    @Test
    fun `접근 코드 제거 - 이전 수신자의 받은 기록함 항목은 즉시 제거`() {
        val registry = SenderRegistry()
        val repository =
            FakeReceiverRepository(
                initialAuthCode = "master-key",
                initialResult = Result.success(listOf(recordBox())),
            )
        val viewModel = ReceivedRecordsViewModel(registry, repository, RecordingErrorReporter())
        assertEquals(1, viewModel.uiState.value.senders.size)

        repository.authCode.value = null

        assertTrue(
            viewModel.uiState.value.senders
                .isEmpty(),
        )
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

private class RecordingErrorReporter : ErrorReporter {
    val attributes = mutableListOf<Map<String, String>>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        this.attributes += attributes
    }
}

private class FakeReceiverRepository(
    initialAuthCode: String?,
    initialResult: Result<List<ReceivedRecordBox>> = Result.success(emptyList()),
) : ReceiverRepository {
    val authCode = MutableStateFlow(initialAuthCode)
    var recordBoxesResult = initialResult
    var recordBoxesCallCount = 0

    override val authCodeFlow: Flow<String?> = authCode

    override suspend fun currentAuthCode(): String? = authCode.value

    override suspend fun saveAuthCode(code: String) {
        authCode.value = code
    }

    override suspend fun clearAuthCode() {
        authCode.value = null
    }

    override suspend fun getReceivedRecordBoxes(): Result<List<ReceivedRecordBox>> {
        recordBoxesCallCount += 1
        return recordBoxesResult
    }

    override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> = emptyFlow()

    override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> = error("unused")

    override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> = error("unused")

    override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> = error("unused")

    override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> = error("unused")

    override suspend fun loadMindRecordsCount(): Result<LoadCountResult> = error("unused")

    override suspend fun loadTimeLettersCount(): Result<LoadCountResult> = error("unused")

    override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> = error("unused")
}

private fun recordBox(recordBoxId: Long = 18L): ReceivedRecordBox =
    ReceivedRecordBox(
        recordBoxId = recordBoxId,
        accessCode = "record-key-$recordBoxId",
        senderName = "김혜성",
        receiverName = "김지은",
        relation = "DAUGHTER",
        recordStatus = ReceivedRecordStatus.Stored,
        viewStatus = ReceivedRecordViewStatus.Viewable,
        verification =
            ReceivedRecordVerification.Approved(
                requestedAt = "2026-07-29T16:58:36",
                approvedAt = "2026-07-30T04:25:42",
            ),
    )

private fun recordBoxEntry(recordBoxId: Long): ReceivedRecordItem =
    ReceivedRecordItem(
        recordBoxId = recordBoxId,
        accessCode = "old-key",
        senderName = "old",
        receiverName = "old-receiver",
        relation = "OTHER",
        recordStatus = ReceivedRecordStatus.Stored,
        viewStatus = ReceivedRecordViewStatus.Requestable,
        verification = ReceivedRecordVerification.NotRequested,
    )
