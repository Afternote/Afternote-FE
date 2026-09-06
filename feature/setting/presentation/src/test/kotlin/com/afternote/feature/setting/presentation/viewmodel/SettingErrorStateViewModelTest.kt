package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingErrorStateViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `설정 메인은 조회 실패 후 재시도 성공 상태로 복구한다`() =
        runTest(dispatcher) {
            var attempts = 0
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyProfile = {
                        attempts += 1
                        if (attempts == 1) error("offline")
                        testUser
                    }
                }
            val viewModel = SettingViewModel(FakeAuthRepository.strict(), repository)

            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is SettingUiState.Error)

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(SettingUiState.Success(testUser.name, testUser.email), viewModel.uiState.value)
        }

    @Test
    fun `프로필은 조회 실패 후 재시도 성공 상태로 복구한다`() =
        runTest(dispatcher) {
            var attempts = 0
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyProfile = {
                        attempts += 1
                        if (attempts == 1) error("offline")
                        testUser
                    }
                }
            val viewModel = ProfileEditViewModel(repository)

            advanceUntilIdle()
            assertEquals(ProfileEditUiState.Error, viewModel.uiState.value)

            viewModel.retryLoadProfile()
            advanceUntilIdle()

            assertEquals(
                ProfileEditUiState.Success(testUser.name, testUser.phone.orEmpty(), testUser.email),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `연결 계정은 조회 실패를 빈 목록이 아닌 오류로 유지하고 재시도한다`() =
        runTest(dispatcher) {
            var attempts = 0
            val repository =
                FakeUserRepository.strict().apply {
                    onGetConnectedAccounts = {
                        attempts += 1
                        if (attempts == 1) error("offline")
                        testConnectedAccount
                    }
                }
            val viewModel = ConnectedAccountsViewModel(repository)

            advanceUntilIdle()
            assertEquals(UiText.Resource(R.string.setting_connected_accounts_load_error), viewModel.uiState.value.errorMessage)
            assertTrue(
                viewModel.uiState.value.accounts
                    .isEmpty(),
            )

            viewModel.retryLoadConnectedAccounts()
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.errorMessage)
            assertEquals(4, viewModel.uiState.value.accounts.size)
        }

    @Test
    fun `연결 계정 변경 실패는 기존 목록을 유지하고 오류 이벤트를 보낸다`() =
        runTest(dispatcher) {
            val repository =
                FakeUserRepository.strict().apply {
                    onGetConnectedAccounts = { testConnectedAccount }
                    onLinkConnectedAccount = { _, _ -> error("offline") }
                }
            val viewModel = ConnectedAccountsViewModel(repository)
            advanceUntilIdle()
            val accountsBeforeMutation = viewModel.uiState.value.accounts
            val event = async { viewModel.events.first() }

            viewModel.link("google", "token")
            advanceUntilIdle()

            assertEquals(accountsBeforeMutation, viewModel.uiState.value.accounts)
            assertEquals(
                ConnectedAccountsEvent.ShowError("계정 연결에 실패했습니다."),
                event.await(),
            )
        }

    @Test
    fun `연결 계정 해제 실패는 기존 목록을 유지하고 오류 이벤트를 보낸다`() =
        runTest(dispatcher) {
            val connectedAccount = testConnectedAccount.copy(google = true, googleEmail = "google@afternote.com")
            val repository =
                FakeUserRepository.strict().apply {
                    onGetConnectedAccounts = { connectedAccount }
                    onUnlinkConnectedAccount = { error("offline") }
                }
            val viewModel = ConnectedAccountsViewModel(repository)
            advanceUntilIdle()
            val accountsBeforeMutation = viewModel.uiState.value.accounts
            val event = async { viewModel.events.first() }

            viewModel.onToggle(provider = "google", enabled = false)
            advanceUntilIdle()

            assertEquals(accountsBeforeMutation, viewModel.uiState.value.accounts)
            assertEquals(
                ConnectedAccountsEvent.ShowError("계정 연결 해제에 실패했습니다."),
                event.await(),
            )
        }

    @Test
    fun `프로필 변경 실패는 기존 폼을 유지하고 오류 이벤트를 보낸다`() =
        runTest(dispatcher) {
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyProfile = { testUser }
                    onUpdateMyProfile = { _, _, _ -> error("offline") }
                }
            val viewModel = ProfileEditViewModel(repository)
            advanceUntilIdle()
            val event = async { viewModel.events.first() }

            viewModel.updateProfile("새 이름", "01000000000")
            advanceUntilIdle()

            assertEquals(
                ProfileEditUiState.Success(testUser.name, testUser.phone.orEmpty(), testUser.email),
                viewModel.uiState.value,
            )
            assertEquals(ProfileEditEvent.UpdateFailure, event.await())
        }

    private companion object {
        val testUser = User("박서연", "test@afternote.com", "01012345678", null)
        val testConnectedAccount =
            UserConnectedAccount(
                local = true,
                google = false,
                naver = false,
                kakao = false,
                apple = false,
                localEmail = "test@afternote.com",
                googleEmail = null,
                naverEmail = null,
                kakaoEmail = null,
                appleEmail = null,
            )
    }
}
