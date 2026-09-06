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
import com.afternote.feature.afternote.presentation.R
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
import java.io.IOException
import java.lang.reflect.Proxy

/**
 * 수신자 선택 결과가 조용히 사라지지 않는지에 대한 회귀 가드 (#1405).
 *
 * 종전 구현은 목록 로드가 실패한 상태에서 `getReceiverById` 가 null 이면 `?: return` 으로 끝나
 * 사용자가 «골랐는데 아무 일도 안 일어남» 을 겪었다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorReceiverSelectionRecoveryTest {
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
    fun `목록을 끝내 받지 못하면 선택 소실을 오류로 알린다`() =
        runTest(dispatcher) {
            val repository = repositoryWith { throw IOException("목록 조회 실패") }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            val resolved = viewModel.resolveSelectedReceiver(RECEIVER_ID)
            runCurrent()

            assertNull("해석하지 못한 선택은 폼에 넣을 값이 없다", resolved)
            assertEquals(
                AfternoteEditorError.ReceiverSelectionUnavailable,
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `캐시가 비어 있어도 다시 받아 선택을 살린다`() =
        runTest(dispatcher) {
            val repository = repositoryWith { listOf(RECEIVER) }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            val resolved = viewModel.resolveSelectedReceiver(RECEIVER_ID)
            runCurrent()

            assertEquals("김수신", resolved?.name)
            assertEquals("딸", resolved?.label)
            assertNull("살아난 선택에는 오류를 세우지 않는다", viewModel.uiState.value.error)
        }

    @Test
    fun `이미 받아 둔 목록으로 해석되면 다시 받지 않는다`() =
        runTest(dispatcher) {
            val repository = repositoryWith { listOf(RECEIVER) }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.refreshAuthorReceivers()
            runCurrent()

            val resolved = viewModel.resolveSelectedReceiver(RECEIVER_ID)
            runCurrent()

            assertEquals(RECEIVER_ID, resolved?.id)
            assertEquals("캐시 적중이면 추가 조회가 없어야 한다", 1, repository.getReceiversCalls)
        }

    @Test
    fun `선택 소실은 저장 실패와 다른 문구로 안내한다`() {
        assertEquals(
            R.string.afternote_editor_receiver_selection_unavailable,
            AfternoteEditorError.ReceiverSelectionUnavailable.messageResId(),
        )
    }

    private fun repositoryWith(receivers: suspend () -> List<Receiver>): FakeUserRepository =
        FakeUserRepository.strict().apply { onGetReceivers = receivers }

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
        const val RECEIVER_ID = 7L
        val RECEIVER = Receiver(RECEIVER_ID, "김수신", "딸", "fake-auth-7")
    }
}
