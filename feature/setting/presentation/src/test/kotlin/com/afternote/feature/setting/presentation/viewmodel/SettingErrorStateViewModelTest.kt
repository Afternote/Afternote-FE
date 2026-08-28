package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
                object : FakeUserRepository() {
                    override suspend fun getMyProfile(): User {
                        attempts += 1
                        if (attempts == 1) error("offline")
                        return testUser
                    }
                }
            val viewModel = SettingViewModel(FakeAuthRepository(), repository)

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
                object : FakeUserRepository() {
                    override suspend fun getMyProfile(): User {
                        attempts += 1
                        if (attempts == 1) error("offline")
                        return testUser
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
                object : FakeUserRepository() {
                    override suspend fun getConnectedAccounts(): UserConnectedAccount {
                        attempts += 1
                        if (attempts == 1) error("offline")
                        return testConnectedAccount
                    }
                }
            val viewModel = ConnectedAccountsViewModel(repository)

            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.errorMessage != null)
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
                object : FakeUserRepository() {
                    override suspend fun getConnectedAccounts(): UserConnectedAccount = testConnectedAccount

                    override suspend fun linkConnectedAccount(
                        provider: String,
                        accessToken: String,
                    ): UserConnectedAccount = error("offline")
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
    fun `프로필 변경 실패는 기존 폼을 유지하고 오류 이벤트를 보낸다`() =
        runTest(dispatcher) {
            val repository =
                object : FakeUserRepository() {
                    override suspend fun getMyProfile(): User = testUser

                    override suspend fun updateMyProfile(
                        name: String?,
                        phone: String?,
                        profileImageUrl: String?,
                    ): User = error("offline")
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

    @Test
    fun `수신자 목록은 예외와 정상 빈 목록을 서로 다른 상태로 표현한다`() =
        runTest(dispatcher) {
            var subscriptions = 0
            val repository =
                object : FakeUserRepository() {
                    override val receiverListFlow: Flow<List<Receiver>>
                        get() =
                            flow {
                                subscriptions += 1
                                if (subscriptions == 1) error("offline")
                                emit(emptyList())
                            }
                }
            val viewModel = ReceiverListViewModel(repository)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

            advanceUntilIdle()
            assertEquals(ReceiverListUiState.Error, viewModel.uiState.value)

            viewModel.retry()
            advanceUntilIdle()

            assertEquals(ReceiverListUiState.Success(emptyList()), viewModel.uiState.value)
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

private open class FakeUserRepository : UserRepository {
    override val receiverListFlow: Flow<List<Receiver>> = flowOf(emptyList())

    override suspend fun getReceivers(): List<Receiver> = error("unused")

    override suspend fun createReceiver(
        name: String,
        relation: String,
        phone: String?,
        email: String?,
        message: String?,
    ): ReceiverCreated = error("unused")

    override suspend fun getReceiverDetail(receiverId: Long): ReceiverDetail = error("unused")

    override suspend fun updateReceiver(
        receiverId: Long,
        name: String,
        phone: String,
        relation: String,
        email: String,
    ): Receiver = error("unused")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        message: String,
    ) = error("unused")

    override suspend fun getMyProfile(): User = error("unused")

    override suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User = error("unused")

    override suspend fun deleteAccount() = error("unused")

    override suspend fun logActivity() = error("unused")

    override suspend fun getMyPushSettings(): UserPushSetting = error("unused")

    override suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting = error("unused")

    override suspend fun getConnectedAccounts(): UserConnectedAccount = error("unused")

    override suspend fun linkConnectedAccount(
        provider: String,
        accessToken: String,
    ): UserConnectedAccount = error("unused")

    override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount = error("unused")

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions = error("unused")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        conditions: List<DeliveryConditionItem>,
    ): ReceiverDeliveryConditions = error("unused")
}

private class FakeAuthRepository : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = flowOf(true)

    override suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun clearSession(): Result<Unit> = Result.success(Unit)

    override suspend fun getAccessToken(): Result<String?> = Result.success(null)

    override suspend fun getRefreshToken(): Result<String?> = Result.success(null)

    override suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession> = error("unused")

    override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> = error("unused")

    override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> = error("unused")

    override suspend fun rotateToken(): Result<TokenBundle> = error("unused")

    override suspend fun logout(): Result<Unit> = Result.success(Unit)
}
