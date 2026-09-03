package com.afternote.feature.onboarding.presentation.login

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 로그인의 **순수 전이** (#1802).
 *
 * 코루틴 하네스가 없다 — 여기서 보는 Intent 는 `loginUseCase` 를 부르지 않고 `dispatch` →
 * `reduce` 만 지난다. 로그인 자체의 비동기 갈래는 [LoginViewModelTest] 가 본다.
 */
class LoginReducerTest {
    private val recorded = mutableListOf<Map<String, String>>()

    private fun viewModel(): LoginViewModel =
        LoginViewModel(
            // strict 페이크라 로그인 경로가 실제로 불리면 그 자리에서 드러난다.
            loginUseCase = LoginUseCase(FakeAuthRepository.strict()),
            errorReporter =
                object : ErrorReporter {
                    override fun writeFailure(
                        throwable: Throwable,
                        attributes: Map<String, String>,
                    ) {
                        recorded += attributes
                    }
                },
        )

    @Test
    fun `입력이 바뀌면 앞선 자격 거절이 지워진다`() {
        val viewModel = viewModel()

        viewModel.onIntent(LoginIntent.UpdateEmail("user@example.com"))
        viewModel.onIntent(LoginIntent.UpdatePassword("pw"))

        val state = viewModel.uiState.value
        assertEquals("user@example.com", state.email)
        assertEquals("pw", state.password)
        assertFalse(state.hasCredentialError)
    }

    @Test
    fun `소비 Intent 는 그 신호만 되돌린다`() {
        val viewModel = viewModel()
        viewModel.onIntent(LoginIntent.UpdateEmail("user@example.com"))

        viewModel.onIntent(LoginIntent.ConsumeLoggedIn)
        viewModel.onIntent(LoginIntent.ConsumeOnboardingStart)
        viewModel.onIntent(LoginIntent.ConsumeError)

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertFalse(state.shouldStartOnboarding)
        assertNull(state.errorMessage)
        assertEquals("user@example.com", state.email)
    }

    @Test
    fun `네트워크 팝업 닫기는 재실행 없이 상태만 바꾼다`() {
        val viewModel = viewModel()

        viewModel.onIntent(LoginIntent.DismissNetworkError)

        assertFalse(viewModel.uiState.value.showNetworkErrorPopup)
    }

    @Test
    fun `소셜 토큰 실패 보고는 계측만 남기고 상태를 바꾸지 않는다`() {
        val viewModel = viewModel()
        val before = viewModel.uiState.value

        viewModel.onIntent(
            LoginIntent.ReportSocialTokenFailure(AuthProvider.GOOGLE, RuntimeException("SDK 실패")),
        )

        assertEquals(before, viewModel.uiState.value)
        assertEquals("social_token_request", recorded.single()["auth_stage"])
        assertEquals("google", recorded.single()["auth_provider"])
    }
}
