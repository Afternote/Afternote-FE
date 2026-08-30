package com.afternote.feature.onboarding.presentation.login

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakePasskeyRepository
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.domain.usecase.auth.PasskeyLoginUseCase
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

/**
 * 로그인 실패가 어떤 키로 기록되는지, 그리고 코루틴 취소가 실패로 취급되지 않는지 검증한다.
 *
 * 리포팅은 실패했을 때만 동작하므로 [FakeAuthRepository] 는 모든 로그인 경로를 실패로 고정한다.
 */
class LoginViewModelReportingTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope 가 Dispatchers.Main 을 쓰므로 테스트 디스패처로 바꿔 실행 시점을 제어한다.
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeErrorReporter : ErrorReporter {
        val written = mutableListOf<Map<String, String>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            written += attributes
        }
    }

    private fun viewModelWith(
        failure: Throwable,
        reporter: ErrorReporter,
    ) = LoginViewModel(
        loginUseCase =
            LoginUseCase(
                FakeAuthRepository.strict().apply {
                    onDefaultLogin = { _, _ -> Result.failure(failure) }
                    onKakaoLogin = { Result.failure(failure) }
                    onGoogleLogin = { Result.failure(failure) }
                },
            ),
        // 이 파일이 보는 것은 이메일·소셜 실패의 기록 키다 — 패스키 경로는 닫아 둔다.
        passkeyLoginUseCase =
            PasskeyLoginUseCase(
                passkeyRepository = FakePasskeyRepository.strict(),
                authRepository = FakeAuthRepository.strict(),
            ),
        errorReporter = reporter,
    )

    @Test
    fun `카카오 로그인 실패는 login 단계와 kakao 수단으로 기록된다`() =
        runTest(dispatcher) {
            val reporter = FakeErrorReporter()
            val viewModel = viewModelWith(RuntimeException("서버 거절"), reporter)

            viewModel.loginWithKakao("oauth-token")
            advanceUntilIdle()

            val attributes = reporter.written.single()
            assertEquals("login", attributes["auth_stage"])
            assertEquals("kakao", attributes["auth_provider"])
        }

    @Test
    fun `이메일 로그인 실패는 email 수단으로 기록된다`() =
        runTest(dispatcher) {
            val reporter = FakeErrorReporter()
            val viewModel = viewModelWith(RuntimeException("서버 거절"), reporter)

            viewModel.loginWithEmail()
            advanceUntilIdle()

            assertEquals("email", reporter.written.single()["auth_provider"])
        }

    @Test
    fun `소셜 토큰 획득 실패는 social_token_request 단계로 기록된다`() =
        runTest(dispatcher) {
            val reporter = FakeErrorReporter()
            val viewModel = viewModelWith(RuntimeException("미사용"), reporter)

            viewModel.onSocialTokenRequestFailed(AuthProvider.GOOGLE, RuntimeException("SDK 실패"))

            val attributes = reporter.written.single()
            assertEquals("social_token_request", attributes["auth_stage"])
            assertEquals("google", attributes["auth_provider"])
        }

    @Test
    fun `코루틴 취소는 기록하지도 실패 상태로 소비하지도 않는다`() =
        runTest(dispatcher) {
            val reporter = FakeErrorReporter()
            // 화면 이탈로 스코프가 취소되면 runCatching 이 이걸 Result.failure 로 만들어 넘긴다.
            val viewModel = viewModelWith(CancellationException("스코프 취소"), reporter)

            viewModel.loginWithKakao("oauth-token")
            advanceUntilIdle()

            assertTrue("취소는 리포팅 대상이 아니다", reporter.written.isEmpty())
            assertNull("취소를 실패 문구로 띄우면 안 된다", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `자격 거절은 기록하지 않는다 - 비밀번호 오타는 장애가 아니다`() =
        runTest(dispatcher) {
            val reporter = FakeErrorReporter()
            val viewModel = viewModelWith(CoreAuthFailure.InvalidLoginCredentials(RuntimeException("401")), reporter)

            viewModel.loginWithEmail()
            advanceUntilIdle()

            assertTrue(reporter.written.isEmpty())
        }
}
