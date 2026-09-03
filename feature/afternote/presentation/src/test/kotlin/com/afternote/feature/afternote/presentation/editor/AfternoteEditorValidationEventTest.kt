package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.editor.state.AfternoteValidationError
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorValidationEventTest {
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
    fun `동일 검증 오류가 소비 전후 반복돼도 새 이벤트가 발행되고 이전 소비가 최신 이벤트를 지우지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.saveInvalidSocialAfternote()
            runCurrent()
            val first = requireNotNull(viewModel.uiState.value.errorEvent)

            // 소비(null)와 다음 동일 오류를 UI 수집기가 각각 관찰하지 못하고 합쳐도, 새 occurrence로 구분돼야 한다.
            viewModel.onErrorConsumed(first)
            viewModel.saveInvalidSocialAfternote()
            runCurrent()
            val second = requireNotNull(viewModel.uiState.value.errorEvent)

            assertEquals(
                AfternoteValidationError.TITLE_REQUIRED,
                (first.error as AfternoteEditorError.Validation).reason,
            )
            assertEquals(first.error, second.error)
            assertNotEquals("같은 오류도 저장 시도마다 별도 UI 이벤트여야 한다", first, second)

            viewModel.saveInvalidSocialAfternote()
            runCurrent()
            val third = requireNotNull(viewModel.uiState.value.errorEvent)
            assertEquals(second.error, third.error)
            assertNotEquals("소비 전 같은 오류도 저장 시도마다 별도 UI 이벤트여야 한다", second, third)

            viewModel.onErrorConsumed(second)
            runCurrent()
            assertEquals("이전 Snackbar의 종료가 최신 이벤트를 지우면 안 된다", third, viewModel.uiState.value.errorEvent)

            viewModel.onErrorConsumed(third)
            runCurrent()
            assertNull(viewModel.uiState.value.errorEvent)
        }

    private fun AfternoteEditorViewModel.saveInvalidSocialAfternote() {
        saveAfternote(
            payload =
                RegisterAfternotePayload(
                    serviceName = "",
                    date = "2026.08.27",
                    accountId = "account",
                    password = "password",
                    processingMethods = listOf("계정 삭제"),
                ),
            selectedReceiverIds = listOf(1L),
            memorialMedia = SaveAfternoteMemorialMedia(),
        )
    }

    private fun viewModel(): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("initialType" to AfternoteType.SOCIAL_NETWORK)),
            userReceiverRepository = repositoryProxy<UserReceiverRepository>(),
            afternoteRepository = repositoryProxy<AfternoteRepository>(),
            memorialThumbnailUploadRepository =
                MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    MemorialMediaUploadRepository { _, _ -> error("미디어 저장이 호출되면 안 됩니다") },
                ),
            errorReporter = repositoryProxy<ErrorReporter>(),
        )

    private inline fun <reified T> repositoryProxy(): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> error("${T::class.java.simpleName}.${method.name} 호출은 이 테스트에서 예상하지 않았습니다") } as T
}
