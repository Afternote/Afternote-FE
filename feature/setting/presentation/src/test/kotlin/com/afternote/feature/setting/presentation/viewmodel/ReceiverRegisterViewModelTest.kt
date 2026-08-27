package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.error.ReceiverRegistrationRejected
import com.afternote.core.domain.testing.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverRegisterViewModelTest {
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
    fun `등록 거절 시 서버 문구를 화면 상태로 전달한다`() =
        runTest(dispatcher) {
            val repository =
                FakeUserRepository.strict().apply {
                    onCreateReceiver = { _, _, _, _, _ ->
                        throw ReceiverRegistrationRejected(
                            serverMessage = "수신자 이메일은 필수입니다.",
                            cause = IllegalStateException("server rejection"),
                        )
                    }
                }
            val viewModel = ReceiverRegisterViewModel(repository)

            viewModel.register(
                name = "수신자",
                relation = "친구",
                phone = null,
                email = "receiver@afternote.com",
                message = null,
            )
            runCurrent()

            assertEquals(
                ReceiverRegisterUiState(
                    isLoading = false,
                    error = ReceiverRegisterError.ServerMessage("수신자 이메일은 필수입니다."),
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `원인을 알 수 없는 등록 실패는 일반 오류 상태로 변환한다`() =
        runTest(dispatcher) {
            val repository =
                FakeUserRepository.strict().apply {
                    onCreateReceiver = { _, _, _, _, _ ->
                        throw IllegalStateException("unexpected failure")
                    }
                }
            val viewModel = ReceiverRegisterViewModel(repository)

            viewModel.register(
                name = "수신자",
                relation = "친구",
                phone = null,
                email = "receiver@afternote.com",
                message = null,
            )
            runCurrent()

            assertEquals(
                ReceiverRegisterUiState(
                    isLoading = false,
                    error = ReceiverRegisterError.Generic,
                ),
                viewModel.uiState.value,
            )
        }
}
