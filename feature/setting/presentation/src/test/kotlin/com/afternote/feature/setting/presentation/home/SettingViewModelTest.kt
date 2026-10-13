package com.afternote.feature.setting.presentation.home

import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.model.user.User
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
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
class SettingViewModelTest {
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
    fun `deleteAccount 성공 시 Loading을 거쳐 Success가 된다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(onDeleteAccount = {})

            viewModel.onIntent(SettingIntent.DeleteAccount)

            assertEquals(WithdrawUiState.Loading, viewModel.uiState.value.withdraw)
            runCurrent()
            assertEquals(WithdrawUiState.Success, viewModel.uiState.value.withdraw)
        }

    @Test
    fun `deleteAccount 실패 시 Error가 되고 오류를 닫으면 Idle로 돌아간다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(onDeleteAccount = { throw IllegalStateException("failure") })

            viewModel.onIntent(SettingIntent.DeleteAccount)
            runCurrent()

            assertEquals(WithdrawUiState.Error, viewModel.uiState.value.withdraw)
            viewModel.onIntent(SettingIntent.DismissWithdrawError)
            assertEquals(WithdrawUiState.Idle, viewModel.uiState.value.withdraw)
        }

    @Test
    fun `Loading 중에는 탈퇴 요청을 중복 실행하지 않는다`() =
        runTest(dispatcher) {
            var requestCount = 0
            val viewModel = viewModel(onDeleteAccount = { requestCount++ })

            viewModel.onIntent(SettingIntent.DeleteAccount)
            viewModel.onIntent(SettingIntent.DeleteAccount)
            runCurrent()

            assertEquals(1, requestCount)
        }

    private fun viewModel(onDeleteAccount: () -> Unit): SettingViewModel =
        SettingViewModel(
            authRepository = FakeAuthRepository.strict(),
            myProfileRepository =
                FakeMyProfileRepository.strict().apply {
                    onGetMyProfile = { User("name", "user@example.com", null, null) }
                },
            accountRepository =
                FakeSettingAccountRepository.strict().apply {
                    this.onDeleteAccount = { onDeleteAccount() }
                },
        )
}
