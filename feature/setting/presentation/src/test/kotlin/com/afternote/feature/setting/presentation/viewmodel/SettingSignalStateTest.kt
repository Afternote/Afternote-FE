package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingSignalStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val user = User("이름", "test@example.com", "01012345678", null)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `수집자가 없어도 저장 성공을 보존하고 소비 뒤 같은 성공을 다시 전달한다`() =
        runTest(dispatcher) {
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyProfile = { user }
                    onUpdateMyProfile = { _, _, _ -> user }
                }
            val viewModel = ProfileEditViewModel(repository)
            runCurrent()

            repeat(2) {
                viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
                runCurrent()
                assertEquals(ProfileEditEvent.UpdateSuccess, (viewModel.uiState.value as ProfileEditUiState.Success).pendingEvent)
                viewModel.onIntent(ProfileEditIntent.ConsumeEvent(ProfileEditEvent.UpdateSuccess))
                assertNull((viewModel.uiState.value as ProfileEditUiState.Success).pendingEvent)
            }
            assertEquals(2, repository.profileUpdateCalls.size)
        }

    @Test
    fun `저장 중 연속 Intent는 요청을 한 번만 보내고 실패 뒤 재시도할 수 있다`() =
        runTest(dispatcher) {
            val pending = CompletableDeferred<Unit>()
            val repository =
                FakeUserRepository.strict().apply {
                    onGetMyProfile = { user }
                    onUpdateMyProfile = { _, _, _ ->
                        pending.await()
                        error("offline")
                    }
                }
            val viewModel = ProfileEditViewModel(repository)
            runCurrent()
            repeat(2) { viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678")) }
            runCurrent()
            assertEquals(1, repository.profileUpdateCalls.size)
            pending.complete(Unit)
            runCurrent()
            assertEquals(ProfileEditEvent.UpdateFailure, (viewModel.uiState.value as ProfileEditUiState.Success).pendingEvent)

            viewModel.onIntent(ProfileEditIntent.ConsumeEvent(ProfileEditEvent.UpdateFailure))
            repository.onUpdateMyProfile = { _, _, _ -> user }
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
            runCurrent()
            assertEquals(ProfileEditEvent.UpdateSuccess, (viewModel.uiState.value as ProfileEditUiState.Success).pendingEvent)
            assertEquals(2, repository.profileUpdateCalls.size)
        }

    @Test
    fun `늦은 오류 소비가 새 계정 연결 신호를 지우지 않는다`() =
        runTest(dispatcher) {
            val viewModel = ConnectedAccountsViewModel(FakeUserRepository.strict())
            runCurrent()
            viewModel.onIntent(ConnectedAccountsIntent.NotifyLinkError("인증 실패"))
            viewModel.onIntent(ConnectedAccountsIntent.Toggle("google", true))
            viewModel.onIntent(ConnectedAccountsIntent.ConsumeEvent(ConnectedAccountsEvent.ShowError("인증 실패")))
            assertEquals(ConnectedAccountsEvent.RequestLink("google"), viewModel.uiState.value.pendingEvent)
            viewModel.onIntent(ConnectedAccountsIntent.ConsumeEvent(ConnectedAccountsEvent.RequestLink("google")))
            assertNull(viewModel.uiState.value.pendingEvent)
        }
}
