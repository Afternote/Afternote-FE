package com.afternote.feature.setting.presentation.account

import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectedAccountsMviTest {
    private val repository = FakeSettingAccountRepository(connectedAccounts = ACCOUNTS)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `토글을 켜면 저장소를 부르지 않고 연결 요청 신호만 올린다`() {
        val viewModel = ConnectedAccountsViewModel(repository)

        viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "google", enabled = true))

        assertEquals("google", viewModel.uiState.value.pendingLinkProvider)
        assertTrue(repository.connectedLinkCalls.isEmpty())
        assertTrue(repository.connectedUnlinkCalls.isEmpty())
    }

    @Test
    fun `토글을 끄면 신호 없이 바로 해제한다`() {
        val viewModel = ConnectedAccountsViewModel(repository)

        viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "naver", enabled = false))

        assertNull(viewModel.uiState.value.pendingLinkProvider)
        assertEquals(listOf("naver"), repository.connectedUnlinkCalls)
        assertFalse(
            viewModel.uiState.value.accounts
                .first { it.provider == "naver" }
                .isConnected,
        )
    }

    @Test
    fun `연결 요청 신호는 소비할 때까지 남고 소비하면 걷힌다`() {
        val viewModel = ConnectedAccountsViewModel(repository)

        viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "kakao", enabled = true))
        // 다른 작업이 지나가도 신호는 그대로다.
        viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "naver", enabled = false))
        assertEquals("kakao", viewModel.uiState.value.pendingLinkProvider)

        viewModel.onIntent(ConnectedAccountsIntent.ConsumeLinkRequest("kakao"))
        assertNull(viewModel.uiState.value.pendingLinkProvider)

        // 소비 뒤 같은 제공자를 다시 켜면 다시 요청한다 (A → null → A).
        viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "kakao", enabled = true))
        assertEquals("kakao", viewModel.uiState.value.pendingLinkProvider)
    }

    @Test
    fun `늦게 도착한 소비가 그사이 올라온 다른 제공자 요청을 지우지 않는다`() {
        val viewModel = ConnectedAccountsViewModel(repository)

        viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "google", enabled = true))
        viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "kakao", enabled = true))
        viewModel.onIntent(ConnectedAccountsIntent.ConsumeLinkRequest("google"))

        assertEquals("kakao", viewModel.uiState.value.pendingLinkProvider)

        viewModel.onIntent(ConnectedAccountsIntent.ConsumeLinkRequest("kakao"))
        assertNull(viewModel.uiState.value.pendingLinkProvider)
    }

    @Test
    fun `연결이 연달아 실패해도 매번 요청하고 실패 문구를 남긴 뒤 성공하면 계정을 갱신한다`() {
        var remainingFailures = 2
        repository.onLinkConnectedAccount = { provider, _ ->
            if (remainingFailures > 0) {
                remainingFailures--
                error("oauth rejected")
            }
            ACCOUNTS.copy(google = provider == "google", googleEmail = "linked@afternote.local")
        }
        val viewModel = ConnectedAccountsViewModel(repository)

        repeat(2) {
            viewModel.onIntent(ConnectedAccountsIntent.Link(provider = "google", accessToken = "token-$it"))
            assertEquals("계정 연결에 실패했습니다.", viewModel.uiState.value.errorMessage)
            assertFalse(
                viewModel.uiState.value.accounts
                    .first { it.provider == "google" }
                    .isConnected,
            )
        }
        viewModel.onIntent(ConnectedAccountsIntent.Link(provider = "google", accessToken = "token-2"))

        assertEquals(
            List(3) { FakeSettingAccountRepository.ConnectedAccountLinkCall("google", "token-$it") },
            repository.connectedLinkCalls.toList(),
        )
        val google =
            viewModel.uiState.value.accounts
                .first { it.provider == "google" }
        assertTrue(google.isConnected)
        assertEquals("linked@afternote.local", google.email)
    }

    @Test
    fun `조회 실패는 로딩을 끝내고 실패 문구를 남긴다`() {
        repository.onGetConnectedAccounts = { error("unavailable") }

        val state = ConnectedAccountsViewModel(repository).uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.accounts.isEmpty())
        assertEquals("계정 정보를 불러올 수 없습니다.", state.errorMessage)
    }

    private companion object {
        val ACCOUNTS =
            UserConnectedAccount(
                local = false,
                naver = true,
                google = false,
                kakao = false,
                apple = false,
                localEmail = null,
                naverEmail = "naver@afternote.local",
                googleEmail = null,
                kakaoEmail = null,
                appleEmail = null,
            )
    }
}
