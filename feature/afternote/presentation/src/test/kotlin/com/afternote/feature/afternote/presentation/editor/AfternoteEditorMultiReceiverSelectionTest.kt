package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.domain.usecase.editor.SaveAfternoteUseCase
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

            viewModel.applySelectedReceivers(listOf(DAUGHTER_ID, FRIEND_ID))
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID, FRIEND_ID),
                viewModel.currentForm().afternoteEditReceivers.map { it.id },
            )
            assertEquals(
                listOf("김수신", "박친구"),
                viewModel.currentForm().afternoteEditReceivers.map { it.name },
            )
        }

    @Test
    fun `이미 폼에 있는 수신자를 다시 확정해도 중복으로 쌓이지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(repositoryWith(DAUGHTER, FRIEND))
            viewModel.addReceiverIfAbsent(DAUGHTER_ID, "김수신", "딸")
            runCurrent()

            viewModel.applySelectedReceivers(listOf(DAUGHTER_ID, FRIEND_ID))
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID, FRIEND_ID),
                viewModel.currentForm().afternoteEditReceivers.map { it.id },
            )
        }

    @Test
    fun `선택에서 빠진 기존 수신자는 폼에서도 빠진다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(repositoryWith(DAUGHTER, FRIEND))
            viewModel.addReceiverIfAbsent(DAUGHTER_ID, "김수신", "딸")
            viewModel.addReceiverIfAbsent(FRIEND_ID, "박친구", "친구")
            runCurrent()

            viewModel.applySelectedReceivers(listOf(FRIEND_ID))
            runCurrent()

            assertEquals(
                listOf(FRIEND_ID),
                viewModel.currentForm().afternoteEditReceivers.map { it.id },
            )
        }

    @Test
    fun `이미 폼에 있는 수신자는 다시 조회하지 않고 반영한다`() =
        runTest(dispatcher) {
            val repository = repositoryWith(DAUGHTER)
            val viewModel = viewModel(repository)
            viewModel.addReceiverIfAbsent(DAUGHTER_ID, "김수신", "딸")
            runCurrent()

            viewModel.applySelectedReceivers(listOf(DAUGHTER_ID))
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID),
                viewModel.currentForm().afternoteEditReceivers.map { it.id },
            )
            assertEquals("폼이 이미 들고 있는 표시값을 두고 재조회할 이유가 없다", 0, repository.getReceiversCalls)
        }

    @Test
    fun `해석하지 못한 수신자만 빠지고 나머지 선택은 반영된다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(repositoryWith(DAUGHTER))
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.applySelectedReceivers(listOf(DAUGHTER_ID, UNKNOWN_ID))
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID),
                viewModel.currentForm().afternoteEditReceivers.map { it.id },
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
            viewModel.addReceiverIfAbsent(DAUGHTER_ID, "김수신", "딸")
            viewModel.addReceiverIfAbsent(FRIEND_ID, "박친구", "친구")
            runCurrent()

            viewModel.applySelectedReceivers(listOf(DAUGHTER_ID, FRIEND_ID))
            runCurrent()

            assertEquals(
                listOf(DAUGHTER_ID, FRIEND_ID),
                viewModel.currentForm().afternoteEditReceivers.map { it.id },
            )
            assertNull("정상 반영에는 오류를 세우지 않는다", viewModel.uiState.value.error)
        }

    private fun repositoryWith(vararg receivers: Receiver): FakeUserRepository =
        FakeUserRepository.strict().apply { onGetReceivers = { receivers.toList() } }

    private fun viewModel(userRepository: FakeUserRepository): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("initialType" to AfternoteType.SOCIAL_NETWORK)),
            userRepository = userRepository,
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
