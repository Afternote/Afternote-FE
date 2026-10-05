package com.afternote.feature.setting.presentation.account

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 연결된 계정 화면이 연결 요청 신호를 받자마자 소비하고, 인증은 신호와 떨어진 화면 코루틴에서
 * 돌리는지 확인한다 (#1502). 플랫폼 인증은 이 환경에서 토큰을 주지 못하므로 연결 호출까지는 가지 않는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConnectedAccountsSignalTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeSettingAccountRepository()
    private val viewModel = ConnectedAccountsViewModel(repository)

    @Test
    fun `화면은 연결 요청 신호를 받으면 곧바로 소비하고 다음 요청도 다시 받는다`() {
        composeRule.setContent {
            AfternoteTheme {
                ConnectedAccountsScreen(onBack = {}, viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { !viewModel.uiState.value.isLoading }

        repeat(2) {
            composeRule.runOnIdle { viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider = "google", enabled = true)) }
            composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.uiState.value.pendingLinkProvider == null }
        }

        assertTrue(repository.connectedLinkCalls.isEmpty())
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
