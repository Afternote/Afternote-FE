package com.afternote.feature.afternote.presentation.editor.receiver

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.feature.afternote.presentation.NoopAuthorErrorReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 수신자 선택 화면 ViewModel 계약 (#540).
 *
 * 이 화면의 목록이 죽으면 신규 작성의 수신자 지정 경로가 통째로 끊긴다(서버는 수신자 최소
 * 1명을 요구한다 — 400 code 1615). 목록 로드 성공·실패·재시도와 복수 선택 규칙(#1426)을 고정한다.
 *
 * `GET users/receivers` 는 액세스 토큰으로 호출자를 식별하므로 userId 를 요구하지 않는다 —
 * userId 부재 시나리오 자체가 존재하지 않는 것이 이 구현의 계약이다(#935 진단 흡수분).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectReceiverViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `진입 시 수신자 목록을 불러와 에디터 표시 모델로 채운다`() =
        runTest {
            val repository =
                FakeUserRepository(
                    receivers =
                        listOf(
                            Receiver(1L, "김혜성", "아들", "auth-1"),
                            Receiver(2L, "박경민", "친구", "auth-2"),
                        ),
                )

            val viewModel = SelectReceiverViewModel(repository, NoopAuthorErrorReporter)
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.loadFailed)
            assertEquals(listOf(1L, 2L), state.receivers.map { it.id })
            assertEquals(listOf("김혜성", "박경민"), state.receivers.map { it.name })
            assertEquals(listOf("아들", "친구"), state.receivers.map { it.label })
        }

    @Test
    fun `로드가 끝나기 전에는 로딩 상태다`() =
        runTest {
            val gate = CompletableDeferred<List<Receiver>>()
            val repository = FakeUserRepository(onGetReceivers = { gate.await() })

            val viewModel = SelectReceiverViewModel(repository, NoopAuthorErrorReporter)
            runCurrent()

            assertTrue(viewModel.uiState.value.isLoading)

            gate.complete(listOf(Receiver(1L, "김혜성", "아들", "auth-1")))
            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                listOf(1L),
                viewModel.uiState.value.receivers
                    .map { it.id },
            )
        }

    @Test
    fun `로드 실패 시 실패 상태를 남기고 텔레메트리에 기록한다`() =
        runTest {
            val reporter = RecordingErrorReporter()
            val repository = FakeUserRepository(onGetReceivers = { error("server down") })

            val viewModel = SelectReceiverViewModel(repository, reporter)
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.loadFailed)
            assertEquals(emptyList<Long>(), state.receivers.map { it.id })
            assertEquals(listOf("receiver_select_load"), reporter.recordedStages)
        }

    @Test
    fun `실패 후 다시 시도가 성공하면 실패 상태를 걷어낸다`() =
        runTest {
            val repository =
                FakeUserRepository(
                    receivers = listOf(Receiver(1L, "김혜성", "아들", "auth-1")),
                    onGetReceivers = { error("server down") },
                )

            val viewModel = SelectReceiverViewModel(repository, NoopAuthorErrorReporter)
            runCurrent()
            assertTrue(viewModel.uiState.value.loadFailed)

            repository.onGetReceivers = null
            viewModel.refresh()
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse(state.loadFailed)
            assertEquals(listOf(1L), state.receivers.map { it.id })
        }

    @Test
    fun `수신자를 탭하면 선택되고 같은 수신자를 다시 탭하면 해제된다`() =
        runTest {
            val viewModel = viewModelWithReceivers()

            viewModel.toggleReceiverSelection(1L)
            assertEquals(listOf(1L), viewModel.uiState.value.selectedReceiverIds)

            viewModel.toggleReceiverSelection(1L)
            assertEquals(emptyList<Long>(), viewModel.uiState.value.selectedReceiverIds)
        }

    @Test
    fun `여러 수신자를 탭하면 탭한 순서대로 함께 선택된다`() =
        runTest {
            val viewModel = viewModelWithReceivers()

            viewModel.toggleReceiverSelection(2L)
            viewModel.toggleReceiverSelection(1L)

            assertEquals(listOf(2L, 1L), viewModel.uiState.value.selectedReceiverIds)
        }

    @Test
    fun `복수 선택 중 하나만 다시 탭하면 그 항목만 해제된다`() =
        runTest {
            val viewModel = viewModelWithReceivers()

            viewModel.toggleReceiverSelection(1L)
            viewModel.toggleReceiverSelection(2L)
            viewModel.toggleReceiverSelection(1L)

            assertEquals(listOf(2L), viewModel.uiState.value.selectedReceiverIds)
        }

    @Test
    fun `이미 폼에 있는 수신자는 선택 상태로 열린다`() =
        runTest {
            val viewModel = viewModelWithReceivers()

            viewModel.applyPreselection(listOf(1L, 2L))

            assertEquals(listOf(1L, 2L), viewModel.uiState.value.selectedReceiverIds)
        }

    @Test
    fun `초기 선택은 최초 한 번만 반영해 사용자가 푼 선택을 되살리지 않는다`() =
        runTest {
            val viewModel = viewModelWithReceivers()

            viewModel.applyPreselection(listOf(1L))
            viewModel.toggleReceiverSelection(1L)
            viewModel.applyPreselection(listOf(1L))

            assertEquals(emptyList<Long>(), viewModel.uiState.value.selectedReceiverIds)
        }

    @Test
    fun `초기 선택과 같은 수신자를 탭해도 중복으로 쌓이지 않는다`() =
        runTest {
            val viewModel = viewModelWithReceivers()

            viewModel.applyPreselection(listOf(1L))
            viewModel.toggleReceiverSelection(1L)
            viewModel.toggleReceiverSelection(1L)

            assertEquals(listOf(1L), viewModel.uiState.value.selectedReceiverIds)
        }

    @Test
    fun `목록 응답이 초기 선택보다 먼저 와 있어도 목록에 없는 초기 선택은 빠진다`() =
        runTest {
            val viewModel = viewModelWithReceivers()

            viewModel.applyPreselection(listOf(1L, 99L))

            assertEquals(listOf(1L), viewModel.uiState.value.selectedReceiverIds)
        }

    @Test
    fun `초기 선택이 목록 응답보다 먼저 들어와도 같은 결과다`() =
        runTest {
            val gate = CompletableDeferred<List<Receiver>>()
            val repository = FakeUserRepository(onGetReceivers = { gate.await() })
            val viewModel = SelectReceiverViewModel(repository, NoopAuthorErrorReporter)
            runCurrent()

            viewModel.applyPreselection(listOf(1L, 99L))
            // 목록이 없는 동안엔 보이는 선택도 없다 — 완료 버튼이 확인 없이 켜지지 않는다.
            assertEquals(emptyList<Long>(), viewModel.uiState.value.selectedReceiverIds)

            gate.complete(
                listOf(
                    Receiver(1L, "김혜성", "아들", "auth-1"),
                    Receiver(2L, "박경민", "친구", "auth-2"),
                ),
            )
            runCurrent()

            assertEquals(listOf(1L), viewModel.uiState.value.selectedReceiverIds)
        }

    @Test
    fun `재조회로 목록에서 사라진 수신자 선택만 해제되고 나머지는 남는다`() =
        runTest {
            val repository =
                FakeUserRepository(
                    receivers =
                        listOf(
                            Receiver(1L, "김혜성", "아들", "auth-1"),
                            Receiver(2L, "박경민", "친구", "auth-2"),
                        ),
                )
            val viewModel = SelectReceiverViewModel(repository, NoopAuthorErrorReporter)
            runCurrent()

            viewModel.toggleReceiverSelection(1L)
            viewModel.toggleReceiverSelection(2L)
            repository.receiverState.value = listOf(Receiver(1L, "김혜성", "아들", "auth-1"))
            viewModel.refresh()
            runCurrent()

            assertEquals(listOf(1L), viewModel.uiState.value.selectedReceiverIds)
        }

    @Test
    fun `재조회 후에도 목록에 남아 있는 수신자 선택은 유지된다`() =
        runTest {
            val repository =
                FakeUserRepository(receivers = listOf(Receiver(1L, "김혜성", "아들", "auth-1")))
            val viewModel = SelectReceiverViewModel(repository, NoopAuthorErrorReporter)
            runCurrent()

            viewModel.toggleReceiverSelection(1L)
            viewModel.refresh()
            runCurrent()

            assertEquals(listOf(1L), viewModel.uiState.value.selectedReceiverIds)
        }

    private fun viewModelWithReceivers(): SelectReceiverViewModel {
        val repository =
            FakeUserRepository(
                receivers =
                    listOf(
                        Receiver(1L, "김혜성", "아들", "auth-1"),
                        Receiver(2L, "박경민", "친구", "auth-2"),
                    ),
            )
        val viewModel = SelectReceiverViewModel(repository, NoopAuthorErrorReporter)
        dispatcher.scheduler.runCurrent()
        return viewModel
    }

    /** [ErrorReporter.recordFailure] 가 남긴 `afternote_stage` 속성만 수집한다. */
    private class RecordingErrorReporter : ErrorReporter {
        val recordedStages = mutableListOf<String>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            attributes["afternote_stage"]?.let(recordedStages::add)
        }
    }
}
