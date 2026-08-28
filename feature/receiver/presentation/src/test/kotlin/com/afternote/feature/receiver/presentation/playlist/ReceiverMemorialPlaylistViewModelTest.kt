package com.afternote.feature.receiver.presentation.playlist

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.model.ReceivedPlaylistDetail
import com.afternote.feature.receiver.domain.model.ReceivedPlaylistSong
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        val detailResults =
            ArrayDeque<Result<ReceivedAfternoteDetail>>(
                listOf(Result.failure(failure), Result.failure(failure)),
            )
        val repository =
            FakeReceiverRepository.strict().apply {
                onGetReceivedAfternoteDetail = { detailResults.removeFirst() }
            }
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
        assertEquals(0, repository.getReceivedAfterNotesCalls)
        assertEquals(1, errorReporter.reportedErrors.size)
        assertEquals(IOException::class.java.name, errorReporter.reportedErrors.single().message)
        assertTrue(errorReporter.reportedErrors.none { it.message?.contains("sensitive") == true })

        viewModel.retry()

        assertEquals(listOf(42L, 42L), repository.requestedDetailIds)
        assertEquals(2, errorReporter.reportedErrors.size)
    }

    @Test
    fun `refreshOnReturn - 진행 중인 최초 로드와 겹치면 건너뛴다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val repository =
                FakeReceiverRepository.strict().apply {
                    onGetReceivedAfternoteDetail = {
                        gate.await()
                        Result.success(memorialDetail(songTitles = listOf("첫 곡")))
                    }
                }
            val viewModel = viewModel(afternoteId = 42L, repository = repository)

            // 최초 진입 화면의 ON_RESUME — init 로드가 아직 도는 중이다.
            viewModel.refreshOnReturn()
            gate.complete(Unit)

            assertEquals(listOf(42L), repository.requestedDetailIds)
            assertTrue(viewModel.uiState.value is ReceiverMemorialPlaylistUiState.Success)
        }

    @Test
    fun `refreshOnReturn - 복귀하면 로딩 없이 새 목록으로 갱신한다`() =
        runTest {
            val detailResults =
                ArrayDeque(
                    listOf(
                        Result.success(memorialDetail(songTitles = listOf("첫 곡"))),
                        Result.success(memorialDetail(songTitles = listOf("첫 곡", "새 곡"))),
                    ),
                )
            val repository =
                FakeReceiverRepository.strict().apply {
                    onGetReceivedAfternoteDetail = { detailResults.removeFirst() }
                }
            val viewModel = viewModel(afternoteId = 42L, repository = repository)
            // 최초 로드 완료(Success) 이후부터의 방출을 수집한다.
            val states = mutableListOf<ReceiverMemorialPlaylistUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { states += it }
            }

            viewModel.refreshOnReturn()

            assertEquals(listOf(42L, 42L), repository.requestedDetailIds)
            val refreshed = states.last() as ReceiverMemorialPlaylistUiState.Success
            assertEquals(listOf("첫 곡", "새 곡"), refreshed.songs.map { it.title })
            // 갱신 중 어느 시점에도 Loading 으로 되돌아가지 않는다 — 재진입마다 스피너가 번쩍이지 않게.
            assertTrue(states.none { it is ReceiverMemorialPlaylistUiState.Loading })
        }

    @Test
    fun `refreshOnReturn - 실패해도 보고 있던 목록을 유지하고 실패는 기록한다`() {
        val detailResults =
            ArrayDeque(
                listOf(
                    Result.success(memorialDetail(songTitles = listOf("첫 곡"))),
                    Result.failure(IOException("일시적 실패")),
                ),
            )
        val repository =
            FakeReceiverRepository.strict().apply {
                onGetReceivedAfternoteDetail = { detailResults.removeFirst() }
            }
        val errorReporter = RecordingErrorReporter()
        val viewModel =
            viewModel(
                afternoteId = 42L,
                repository = repository,
                errorReporter = errorReporter,
            )

        viewModel.refreshOnReturn()

        // 잘 보고 있던 목록이 에러 화면으로 대체되지 않는다.
        val state = viewModel.uiState.value as ReceiverMemorialPlaylistUiState.Success
        assertEquals(listOf("첫 곡"), state.songs.map { it.title })
        // 화면에 안 보이는 실패인 만큼 콘솔 기록은 남긴다.
        assertEquals(1, errorReporter.reportedErrors.size)
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

private fun memorialDetail(songTitles: List<String>): ReceivedAfternoteDetail =
    ReceivedAfternoteDetail(
        type = AfternoteType.MEMORIAL,
        serviceName = "추억 플레이리스트",
        senderName = "김발신",
        playlist =
            ReceivedPlaylistDetail(
                songs = songTitles.map { title -> ReceivedPlaylistSong(title = title, artist = "가수", coverUrl = null) },
            ),
    )

private class RecordingErrorReporter : ErrorReporter {
    val reportedErrors = mutableListOf<Throwable>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        reportedErrors += throwable
    }
}
