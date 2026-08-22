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
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
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
    fun `저장된 접근 코드 없음 - 임시 카드만 유지하고 서버는 호출하지 않음`() {
        val registry = SenderRegistry()
        val pending = registry.register("엄마")
        val repository = FakeReceiverRepository(initialAuthCode = null)

        val viewModel = ReceivedRecordsViewModel(registry, repository, RecordingErrorReporter())

        assertEquals(listOf(pending), viewModel.uiState.value.senders)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.hasLoadError)
        assertEquals(0, repository.recordBoxesCallCount)
    }

    @Test
    fun `접근 코드 있음 - 이전 서버 카드와 미인증 로컬 카드를 서버 목록으로 완전히 교체`() {
        val registry = SenderRegistry()
        registry.replaceServerEntries(listOf(serverEntry(id = "stale", receiverId = 1L)))
        registry.register("이전 사용자의 미인증 카드")
        val recordBox = recordBox(receiverId = 18L)
        val repository = FakeReceiverRepository(initialAuthCode = "master-key", initialResult = Result.success(listOf(recordBox)))

        val viewModel = ReceivedRecordsViewModel(registry, repository, RecordingErrorReporter())

        val senders = viewModel.uiState.value.senders
        assertEquals(listOf("record-box:18"), senders.map(SenderEntry::id))
        assertEquals("김혜성", senders.first().name)
        assertEquals("record-key-18", senders.first().authCode)
        assertEquals(ReceivedRecordStatus.Stored, senders.first().recordStatus)
        assertEquals(ReceivedRecordViewStatus.Viewable, senders.first().viewStatus)
        assertEquals("2026-07-30T04:25:42", senders.first().approvedAt)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.hasLoadError)
        assertEquals(1, repository.recordBoxesCallCount)
    }

    @Test
    fun `목록 실패 후 재시도 - 오류를 기록하고 성공 시 서버 카드로 복구`() {
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

        repository.recordBoxesResult = Result.success(listOf(recordBox(receiverId = 20L)))
        viewModel.retry()

        assertEquals(
            listOf("record-box:20"),
            viewModel.uiState.value.senders
                .map(SenderEntry::id),
        )
        assertFalse(viewModel.uiState.value.hasLoadError)
        assertEquals(2, repository.recordBoxesCallCount)
    }

    @Test
    fun `접근 코드 제거 - 이전 수신자의 서버 카드는 즉시 제거`() {
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

private fun recordBox(receiverId: Long = 18L): ReceivedRecordBox =
    ReceivedRecordBox(
        receiverId = receiverId,
        accessCode = "record-key-$receiverId",
        senderName = "김혜성",
        receiverName = "김지은",
        relation = "DAUGHTER",
        recordStatus = ReceivedRecordStatus.Stored,
        viewStatus = ReceivedRecordViewStatus.Viewable,
        verificationStatus = DeliveryVerificationStatus.APPROVED,
        requestedAt = "2026-07-29T16:58:36",
        approvedAt = "2026-07-30T04:25:42",
    )

private fun serverEntry(
    id: String,
    receiverId: Long,
): SenderEntry =
    SenderEntry(
        id = id,
        name = "old",
        receiverId = receiverId,
        recordStatus = ReceivedRecordStatus.Stored,
    )
