package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.model.user.Receiver
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.domain.usecase.editor.SaveAfternoteUseCase
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

/**
 * 수신자 선택 화면이 돌려준 «확정된 전체» 를 폼에 반영하는 계약 (#1426).
 *
 * 반환 채널이 단일 `Long` 이던 시절엔 한 번 진입에 한 명만 담겼다. 복수로 열면서 반영 규칙도
 * «추가» 에서 «교체» 로 바뀐다 — 화면이 폼의 현재 수신자를 선택 상태로 열기 때문에, 거기서 푼
 * 수신자가 폼에 남으면 사용자의 해제가 조용히 무시된다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorMultiReceiverSelectionTest {
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
    fun `선택 화면 한 번으로 수신자 여럿을 폼에 담는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(repositoryWith(DAUGHTER, FRIEND))
            runCurrent()

            viewModel.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(DAUGHTER_ID, FRIEND_ID)))
            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID, FRIEND_ID),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )
            assertEquals(
                listOf("김수신", "박친구"),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.name },
            )
        }

    @Test
    fun `이미 폼에 있는 수신자를 다시 확정해도 중복으로 쌓이지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(repositoryWith(DAUGHTER, FRIEND))
            viewModel.onIntent(AfternoteEditorIntent.AddReceiverIfAbsent(DAUGHTER_ID, "김수신", "딸"))
            runCurrent()

            viewModel.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(DAUGHTER_ID, FRIEND_ID)))
            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID, FRIEND_ID),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )
        }

    @Test
    fun `선택에서 빠진 기존 수신자는 폼에서도 빠진다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(repositoryWith(DAUGHTER, FRIEND))
            viewModel.onIntent(AfternoteEditorIntent.AddReceiverIfAbsent(DAUGHTER_ID, "김수신", "딸"))
            viewModel.onIntent(AfternoteEditorIntent.AddReceiverIfAbsent(FRIEND_ID, "박친구", "친구"))
            runCurrent()

            viewModel.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(FRIEND_ID)))
            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()

            assertEquals(
                listOf(FRIEND_ID),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )
        }

    @Test
    fun `이미 폼에 있는 수신자는 다시 조회하지 않고 반영한다`() =
        runTest(dispatcher) {
            val repository = repositoryWith(DAUGHTER)
            val viewModel = viewModel(repository)
            viewModel.onIntent(AfternoteEditorIntent.AddReceiverIfAbsent(DAUGHTER_ID, "김수신", "딸"))
            runCurrent()

            viewModel.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(DAUGHTER_ID)))
            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )
            assertEquals("폼이 이미 들고 있는 표시값을 두고 재조회할 이유가 없다", 0, repository.getReceiversCalls)
        }

    @Test
    fun `해석하지 못한 수신자만 빠지고 나머지 선택은 반영된다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(repositoryWith(DAUGHTER))
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(DAUGHTER_ID, UNKNOWN_ID)))
            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )
            assertEquals(
                AfternoteEditorError.ReceiverSelectionUnavailable,
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `선택을 그대로 확정하면 폼도 그대로다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(repositoryWith(DAUGHTER, FRIEND))
            backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.onIntent(AfternoteEditorIntent.AddReceiverIfAbsent(DAUGHTER_ID, "김수신", "딸"))
            viewModel.onIntent(AfternoteEditorIntent.AddReceiverIfAbsent(FRIEND_ID, "박친구", "친구"))
            runCurrent()

            viewModel.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(DAUGHTER_ID, FRIEND_ID)))
            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID, FRIEND_ID),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )
            assertNull("정상 반영에는 오류를 세우지 않는다", viewModel.uiState.value.error)
        }

    @Test
    fun `선택 결과는 편집기 복귀에서 한 번만 폼에 적용한다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(repositoryWith(DAUGHTER))
            viewModel.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(DAUGHTER_ID)))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.form.afternoteEditReceivers
                    .isEmpty(),
            )

            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()
            assertEquals(
                listOf(DAUGHTER_ID),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )

            viewModel.onIntent(AfternoteEditorIntent.DeleteReceiver(DAUGHTER_ID))
            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.form.afternoteEditReceivers
                    .isEmpty(),
            )
        }

    @Test
    fun `확정된 수신자 선택은 흐름 복원 뒤에도 한 번 적용한다`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(repositoryWith(DAUGHTER), handle)
            first.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(DAUGHTER_ID)))

            val restored = viewModel(repositoryWith(DAUGHTER), handle)
            restored.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()
            assertEquals(
                listOf(DAUGHTER_ID),
                restored.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )
        }

    @Test
    fun `취소 뒤 늦게 도착한 이전 조회는 최신 선택과 수신자 목록을 덮지 않는다`() =
        runTest(dispatcher) {
            val previousLookup = CompletableDeferred<List<Receiver>>()
            var calls = 0
            val repository =
                FakeUserReceiverRepository.strict().apply {
                    onGetReceivers = {
                        if (calls++ == 0) {
                            withContext(NonCancellable) { previousLookup.await() }
                        } else {
                            listOf(FRIEND)
                        }
                    }
                }
            val viewModel = viewModel(repository)
            viewModel.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(DAUGHTER_ID)))
            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()

            viewModel.onIntent(AfternoteEditorIntent.ReceiversSelected(listOf(FRIEND_ID)))
            viewModel.onIntent(AfternoteEditorIntent.ApplyPendingReceiverSelection)
            runCurrent()
            assertEquals(
                listOf(FRIEND_ID),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )

            previousLookup.complete(listOf(DAUGHTER))
            runCurrent()
            assertEquals(
                listOf(FRIEND_ID),
                viewModel.uiState.value.form.afternoteEditReceivers
                    .map { it.id },
            )
            assertEquals(
                listOf(FRIEND_ID),
                viewModel.uiState.value.authorReceivers
                    .map { it.id },
            )
            assertNull(viewModel.uiState.value.error)
        }

    private fun repositoryWith(vararg receivers: Receiver): FakeUserReceiverRepository =
        FakeUserReceiverRepository.strict().apply { onGetReceivers = { receivers.toList() } }

    private fun viewModel(
        userRepository: FakeUserReceiverRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            route = AfternoteRoute.EditorFlowRoute(initialType = AfternoteType.SOCIAL_NETWORK),
            savedStateHandle = savedStateHandle,
            userReceiverRepository = userRepository,
            afternoteRepository = unusedProxy<AfternoteRepository>(),
            memorialThumbnailUploadRepository =
                MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    MemorialMediaUploadRepository { _, _ -> error("미디어 저장이 호출되면 안 됩니다") },
                ),
            saveAfternoteUseCase = SaveAfternoteUseCase(unusedProxy<AfternoteRepository>()),
            errorReporter = NoopErrorReporter,
        )

    private inline fun <reified T> unusedProxy(): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> error("${T::class.java.simpleName}.${method.name} 호출은 이 테스트에서 예상하지 않았습니다") } as T

    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private companion object {
        const val DAUGHTER_ID = 7L
        const val FRIEND_ID = 11L
        const val UNKNOWN_ID = 99L
        val DAUGHTER = Receiver(DAUGHTER_ID, "김수신", "딸", "fake-auth-7")
        val FRIEND = Receiver(FRIEND_ID, "박친구", "친구", "fake-auth-11")
    }
}
