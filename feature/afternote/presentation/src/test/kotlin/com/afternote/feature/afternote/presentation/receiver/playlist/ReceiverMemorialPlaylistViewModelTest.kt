package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItem
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverMemorialPlaylistViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `상세 조회 실패는 예외 원문 대신 재시도 가능한 앱 오류로 노출한다`() {
        val failure = IOException("sensitive transport failure")
        val repository =
            FakeReceiverRepository(
                detailResults = ArrayDeque(listOf(Result.failure(failure), Result.failure(failure))),
            )
        val errorReporter = RecordingErrorReporter()

        val viewModel =
            viewModel(
                afternoteId = 42L,
                repository = repository,
                errorReporter = errorReporter,
            )

        assertEquals(
            ReceiverMemorialPlaylistUiState.Error(
                messageRes = R.string.receiver_memorial_playlist_load_error,
            ),
            viewModel.uiState.value,
        )
        assertEquals(listOf(42L), repository.requestedDetailIds)
        assertEquals(0, repository.listRequestCount)
        assertEquals(1, errorReporter.reportedErrors.size)
        assertEquals(IOException::class.java.name, errorReporter.reportedErrors.single().message)
        assertTrue(errorReporter.reportedErrors.none { it.message?.contains("sensitive") == true })

        viewModel.retry()

        assertEquals(listOf(42L, 42L), repository.requestedDetailIds)
        assertEquals(2, errorReporter.reportedErrors.size)
    }

    private fun viewModel(
        afternoteId: Long,
        repository: ReceiverRepository,
        errorReporter: ErrorReporter = RecordingErrorReporter(),
    ): ReceiverMemorialPlaylistViewModel =
        ReceiverMemorialPlaylistViewModel(
            savedStateHandle = SavedStateHandle(mapOf("afternoteId" to afternoteId)),
            receiverRepository = repository,
            errorReporter = errorReporter,
        )
}

private class RecordingErrorReporter : ErrorReporter {
    val reportedErrors = mutableListOf<Throwable>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        reportedErrors += throwable
    }
}

private class FakeReceiverRepository(
    private val detailResults: ArrayDeque<Result<ReceivedAfternoteDetail>> = ArrayDeque(),
) : ReceiverRepository {
    override val authCodeFlow: Flow<String?> = emptyFlow()

    var listRequestCount: Int = 0
        private set

    val requestedDetailIds = mutableListOf<Long>()

    override suspend fun currentAuthCode(): String? = null

    override suspend fun saveAuthCode(code: String) = Unit

    override suspend fun clearAuthCode() = Unit

    override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> = emptyFlow()

    override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> {
        listRequestCount += 1
        return unexpectedCall()
    }

    override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> {
        requestedDetailIds += afternoteId
        return detailResults.removeFirst()
    }

    override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> = unexpectedCall()

    override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> = unexpectedCall()

    override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> = unexpectedCall()

    private fun <T> unexpectedCall(): Result<T> = Result.failure(AssertionError("이 테스트에서 호출되면 안 되는 ReceiverRepository 경로입니다."))
}
