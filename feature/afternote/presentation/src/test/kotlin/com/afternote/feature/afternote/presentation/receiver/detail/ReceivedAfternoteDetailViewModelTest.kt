package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** 수신 상세 재진입 갱신([ReceivedAfternoteDetailViewModel.refreshOnReturn]) 계약 가드 (#701). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceivedAfternoteDetailViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `첫 진입 resume 은 재조회를 트리거하지 않는다 - 실패한 init 로드의 에러 화면과 수동 재시도가 살아남는다`() =
        runTest {
            // ReceiverAdvancedAndroidTest.receivedDetail_failureThenRetry 가 잡은 CI 회귀 시나리오 —
            // init 로드가 실패로 끝난 직후의 첫 ON_RESUME 이 자동 재조회로 성공 응답을 먼저 소비하면
            // 에러 화면과 «다시 시도하기» 가 통째로 건너뛰어진다.
            val results =
                ArrayDeque<Result<ReceivedAfternoteDetail>>(
                    listOf(
                        Result.failure(IllegalStateException("offline")),
                        Result.success(receivedDetail(serviceName = "Instagram")),
                    ),
                )
            val repository =
                FakeReceiverRepository.strict().apply {
                    onGetReceivedAfternoteDetail = { results.removeFirst() }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            // 첫 진입 화면의 ON_RESUME (init 로드는 이미 실패로 종료됨) — 재조회가 걸리면 안 된다.
            viewModel.refreshOnReturn()
            assertEquals(listOf(42L), repository.requestedDetailIds)
            assertTrue(states.last() is ReceivedAfternoteDetailUiState.Error)

            // 복구는 사용자의 수동 재시도로만 일어난다.
            viewModel.retry()
            assertEquals(listOf(42L, 42L), repository.requestedDetailIds)
            assertEquals("Instagram", states.last().serviceNameOrNull())
        }

    @Test
    fun `refreshOnReturn - 진행 중인 로드와 겹치면 건너뛴다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val repository =
                FakeReceiverRepository.strict().apply {
                    onGetReceivedAfternoteDetail = {
                        gate.await()
                        Result.success(receivedDetail(serviceName = "Instagram"))
                    }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            // init 로드가 아직 도는 중 — 첫 resume(스킵) 뒤 또 한 번 resume 이 와도 중복이 없어야 한다.
            viewModel.refreshOnReturn()
            viewModel.refreshOnReturn()
            gate.complete(Unit)

            assertEquals(listOf(42L), repository.requestedDetailIds)
            assertEquals("Instagram", states.last().serviceNameOrNull())
        }

    @Test
    fun `refreshOnReturn - 복귀하면 로딩 없이 새 상세로 갱신한다`() =
        runTest {
            val details =
                ArrayDeque(
                    listOf(
                        Result.success(receivedDetail(serviceName = "Instagram")),
                        Result.success(receivedDetail(serviceName = "Facebook")),
                    ),
                )
            val repository =
                FakeReceiverRepository.strict().apply {
                    onGetReceivedAfternoteDetail = { details.removeFirst() }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME

            assertEquals(listOf(42L, 42L), repository.requestedDetailIds)
            assertEquals("Facebook", states.last().serviceNameOrNull())
            // 첫 Success 이후 Loading 을 다시 방출하지 않는다 — 재진입마다 스피너가 번쩍이지 않게.
            val firstSuccess = states.indexOfFirst { it is ReceivedAfternoteDetailUiState.Success }
            assertTrue(states.drop(firstSuccess).none { it is ReceivedAfternoteDetailUiState.Loading })
        }

    @Test
    fun `refreshOnReturn - 실패해도 보고 있던 상세를 유지하고 실패는 기록한다`() =
        runTest {
            val results =
                ArrayDeque(
                    listOf(
                        Result.success(receivedDetail(serviceName = "Instagram")),
                        Result.failure(IOException("일시적 실패")),
                    ),
                )
            val repository =
                FakeReceiverRepository.strict().apply {
                    onGetReceivedAfternoteDetail = { results.removeFirst() }
                }
            val reporter = RecordingErrorReporter()
            val viewModel = viewModel(repository, errorReporter = reporter)
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME

            // 잘 보고 있던 상세가 에러 화면으로 대체되지 않는다.
            assertEquals("Instagram", states.last().serviceNameOrNull())
            assertTrue(states.none { it is ReceivedAfternoteDetailUiState.Error })
            // 화면에 안 보이는 실패인 만큼 콘솔 기록은 남긴다.
            assertEquals(1, reporter.reportedErrors.size)
        }

    @Test
    fun `retry 가 자른 옛 로드는 값을 들고 돌아와도 새 화면을 덮지 않는다`() =
        runTest {
            // 취소는 repository 의 `runCatchingCancellable` 이 대개 먼저 걷어내지만, 조회가 값으로
            // 끝난 뒤에 취소가 들어오는 창이 남는다. 실기에서는 좁아 재현이 들쭉날쭉하므로
            // NonCancellable 로 그 창을 넓혀 결정적으로 만든다 — 가드가 없으면 방출이
            // [Loading, Fresh, Stale] 이 되어 사용자가 방금 부른 화면이 옛 값으로 덮인다.
            val staleGate = CompletableDeferred<Unit>()
            var call = 0
            val repository =
                FakeReceiverRepository.strict().apply {
                    onGetReceivedAfternoteDetail = {
                        if (call++ == 0) {
                            withContext(NonCancellable) { staleGate.await() }
                            Result.success(receivedDetail(serviceName = "Stale"))
                        } else {
                            Result.success(receivedDetail(serviceName = "Fresh"))
                        }
                    }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            // 첫 로드가 매달린 사이 사용자가 «다시 시도하기» — 옛 Job 은 잘리고 새 로드가 화면을 채운다.
            viewModel.retry()
            assertEquals("Fresh", states.last().serviceNameOrNull())

            // 잘린 옛 로드가 이제야 값을 들고 돌아온다.
            staleGate.complete(Unit)

            assertEquals("Fresh", states.last().serviceNameOrNull())
            assertTrue(states.none { it.serviceNameOrNull() == "Stale" })
        }

    private fun TestScope.recordStates(viewModel: ReceivedAfternoteDetailViewModel): List<ReceivedAfternoteDetailUiState> {
        val states = mutableListOf<ReceivedAfternoteDetailUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states += it }
        }
        return states
    }

    private fun viewModel(
        repository: FakeReceiverRepository,
        errorReporter: ErrorReporter = RecordingErrorReporter(),
    ): ReceivedAfternoteDetailViewModel =
        ReceivedAfternoteDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("afternoteId" to 42L)),
            receiverRepository = repository,
            errorReporter = errorReporter,
        )
}

private fun ReceivedAfternoteDetailUiState.serviceNameOrNull(): String? =
    ((this as? ReceivedAfternoteDetailUiState.Success)?.contentUiModel as? ReceivedDetailContentUiModel.SocialNetwork)
        ?.content
        ?.serviceName

private fun receivedDetail(serviceName: String): ReceivedAfternoteDetail =
    ReceivedAfternoteDetail(
        type = AfternoteType.SOCIAL_NETWORK,
        serviceName = serviceName,
        senderName = "이발신",
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
