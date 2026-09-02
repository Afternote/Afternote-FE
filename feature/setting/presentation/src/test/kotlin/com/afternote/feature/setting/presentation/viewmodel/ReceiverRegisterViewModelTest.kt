package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.error.ReceiverRequestRejectedException
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverRegisterViewModelTest {
    private val createCalls = AtomicInteger()

    @Before
    fun setUp() {
        createCalls.set(0)
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invalid email is rejected before repository call`() {
        val viewModel = ReceiverRegisterViewModel(repository())

        viewModel.register("홍길동", "딸", "01012345678", "invalid", null)

        assertEquals(UiText.Resource(R.string.receiver_email_invalid), viewModel.uiState.value.errorMessage)
        assertEquals(0, createCalls.get())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `blank email is rejected as required before repository call`() {
        val viewModel = ReceiverRegisterViewModel(repository())

        viewModel.register("홍길동", "딸", null, "", null)

        assertEquals(UiText.Resource(R.string.receiver_email_required), viewModel.uiState.value.errorMessage)
        assertEquals(0, createCalls.get())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `blank phone is rejected as required before repository call`() {
        val viewModel = ReceiverRegisterViewModel(repository())

        viewModel.register("홍길동", "딸", null, "receiver@example.com", null)

        assertEquals(UiText.Resource(R.string.receiver_phone_required), viewModel.uiState.value.errorMessage)
        assertEquals(0, createCalls.get())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `invalid phone is rejected before repository call`() {
        val viewModel = ReceiverRegisterViewModel(repository())

        viewModel.register("홍길동", "딸", "123", "receiver@example.com", null)

        assertEquals(UiText.Resource(R.string.receiver_phone_invalid), viewModel.uiState.value.errorMessage)
        assertEquals(0, createCalls.get())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `server rejected message is not shown verbatim to the user`() {
        val serverMessage = "수신자 이메일은 필수입니다."
        val error = ReceiverRequestRejectedException(serverMessage, Exception("origin"))

        assertEquals(
            UiText.Resource(R.string.receiver_request_rejected),
            error.toReceiverFailureMessage(R.string.receiver_register_failed),
        )
    }

    @Test
    fun `unexpected failure uses safe resource message`() {
        val viewModel = ReceiverRegisterViewModel(repository(failure = Exception("internal details")))

        viewModel.register("홍길동", "딸", "01012345678", "receiver@example.com", null)

        assertEquals(UiText.Resource(R.string.receiver_register_failed), viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun repository(failure: Throwable? = null): UserRepository =
        Proxy.newProxyInstance(
            UserRepository::class.java.classLoader,
            arrayOf(UserRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "createReceiver" -> {
                    createCalls.incrementAndGet()
                    failure?.let { throw it }
                    ReceiverCreated(receiverId = 1L, authCode = "auth")
                }

                else -> {
                    error("Unexpected repository call: ${method.name}")
                }
            }
        } as UserRepository
}
