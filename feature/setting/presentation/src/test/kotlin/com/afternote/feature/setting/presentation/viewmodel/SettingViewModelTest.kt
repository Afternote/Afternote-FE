package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
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

    @Test
    fun `로그아웃 완료는 수집자 없이 보존하고 소비 후 같은 성공을 다시 전달한다`() =
        runTest(dispatcher) {
            val auth = FakeAuthRepository.strict().apply { onLogout = { Result.success(Unit) } }
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyProfile = { User("name", "user@example.com", null, null) }
                }
            val viewModel = SettingViewModel(auth, repository)
            viewModel.onIntent(SettingIntent.Logout)
            viewModel.onIntent(SettingIntent.Logout)
            runCurrent()
            assertEquals(1, auth.logoutCalls)
            assertEquals(Unit, viewModel.uiState.value.logoutCompleted)
            viewModel.onIntent(SettingIntent.Refresh)
            runCurrent()
            assertEquals(Unit, viewModel.uiState.value.logoutCompleted)
            viewModel.onIntent(SettingIntent.ConsumeLogoutSuccess)
            assertEquals(null, viewModel.uiState.value.logoutCompleted)
            viewModel.onIntent(SettingIntent.Logout)
            runCurrent()
            assertEquals(2, auth.logoutCalls)
            assertEquals(Unit, viewModel.uiState.value.logoutCompleted)
        }

    @Test
    fun `탈퇴 성공 후 늦은 오류 닫기와 중복 요청은 완료 상태를 유지한다`() =
        runTest(dispatcher) {
            var requests = 0
            val viewModel = viewModel(onDeleteAccount = { requests++ })
            viewModel.onIntent(SettingIntent.DeleteAccount)
            runCurrent()
            viewModel.onIntent(SettingIntent.DismissWithdrawError)
            viewModel.onIntent(SettingIntent.DeleteAccount)
            runCurrent()
            assertEquals(1, requests)
            assertEquals(WithdrawUiState.Success, viewModel.uiState.value.withdraw)
        }

    private fun viewModel(onDeleteAccount: () -> Unit): SettingViewModel =
        SettingViewModel(
            authRepository = FakeAuthRepository.strict(),
            userRepository =
                FakeUserRepository.strict().apply {
                    onGetMyProfile = { User("name", "user@example.com", null, null) }
                    this.onDeleteAccount = { onDeleteAccount() }
                },
        )
}
