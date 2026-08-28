package com.afternote.feature.afternote.presentation.author.detail

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.presentation.author.NoopAuthorErrorReporter
import com.afternote.feature.afternote.presentation.author.afternoteAuthorUserRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

/** 상세 재진입 갱신([AfternoteDetailViewModel.refreshOnReturn]) 계약 가드 (#701). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteDetailViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshOnReturn - 진행 중인 최초 로드와 겹치면 건너뛴다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = {
                        gate.await()
                        Result.success(detail(serviceName = "Instagram"))
                    }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            // 최초 진입 화면의 ON_RESUME — init 로드가 아직 도는 중이다.
            viewModel.refreshOnReturn()
            gate.complete(Unit)

            assertEquals(listOf(73L), repository.requestedDetailIds)
            assertEquals("Instagram", states.last().serviceNameOrNull())
        }

    @Test
    fun `refreshOnReturn - 복귀하면 로딩 없이 새 상세로 갱신한다`() =
        runTest {
            val details =
                ArrayDeque(
                    listOf(
                        Result.success(detail(serviceName = "Instagram")),
                        Result.success(detail(serviceName = "Facebook")),
                    ),
                )
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { details.removeFirst() }
                }
            val viewModel = viewModel(repository)
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn()

            assertEquals(listOf(73L, 73L), repository.requestedDetailIds)
            assertEquals("Facebook", states.last().serviceNameOrNull())
            // 첫 Success 이후 Loading 을 다시 방출하지 않는다 — 재진입마다 스피너가 번쩍이지 않게.
            val firstSuccess = states.indexOfFirst { it is AfternoteDetailUiState.Success }
            assertTrue(states.drop(firstSuccess).none { it is AfternoteDetailUiState.Loading })
        }

    @Test
    fun `refreshOnReturn - 실패해도 보고 있던 상세를 유지하고 실패는 기록한다`() =
        runTest {
            val results =
                ArrayDeque(
                    listOf(
                        Result.success(detail(serviceName = "Instagram")),
                        Result.failure(IOException("일시적 실패")),
                    ),
                )
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { results.removeFirst() }
                }
            val reporter = RecordingErrorReporter()
            val viewModel = viewModel(repository, errorReporter = reporter)
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn()

            // 잘 보고 있던 상세가 에러 화면으로 대체되지 않는다.
            assertEquals("Instagram", states.last().serviceNameOrNull())
            assertTrue(states.none { it is AfternoteDetailUiState.Error })
            // 화면에 안 보이는 실패인 만큼 콘솔 기록은 남긴다.
            assertEquals(1, reporter.reportedErrors.size)
        }

    private fun TestScope.recordStates(viewModel: AfternoteDetailViewModel): List<AfternoteDetailUiState> {
        val states = mutableListOf<AfternoteDetailUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states += it }
        }
        return states
    }

    private fun viewModel(
        repository: FakeAfternoteRepository,
        errorReporter: ErrorReporter = NoopAuthorErrorReporter,
    ): AfternoteDetailViewModel =
        AfternoteDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("itemId" to 73L)),
            afternoteRepository = repository,
            userRepository = afternoteAuthorUserRepository(),
            errorReporter = errorReporter,
        )
}

private fun AfternoteDetailUiState.serviceNameOrNull(): String? =
    ((this as? AfternoteDetailUiState.Success)?.contentUiModel as? DetailContentUiModel.SocialNetwork)
        ?.content
        ?.serviceName

private fun detail(serviceName: String): Detail =
    Detail(
        id = 73L,
        serviceName = serviceName,
        timestamps = DetailTimestamps(updatedAt = "2026.08.22"),
        receivers = emptyList(),
        leaveMessageBlocks = emptyList(),
        content =
            DetailContent.SocialNetwork(
                credentials = DetailCredentials(id = "id@example.test", password = "pw"),
                processingMethods = listOf("계정 삭제"),
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
