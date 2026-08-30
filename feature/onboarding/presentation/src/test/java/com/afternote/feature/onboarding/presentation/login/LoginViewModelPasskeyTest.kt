package com.afternote.feature.onboarding.presentation.login

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakePasskeyRepository
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.domain.usecase.auth.PasskeyLoginUseCase
import com.afternote.core.model.PasskeyAuthenticationOptions
import com.afternote.core.model.Session
import com.afternote.core.ui.UiText
import com.afternote.feature.onboarding.presentation.R
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
import java.net.UnknownHostException

/**
 * 패스키 로그인 상태·기록 계약 회귀 가드 (#764).
 *
 * 계약 —
 * 1. 화면 진입 자동 시도는 **한 번만** 건다(구성 변경으로 재호출돼도 선택기를 다시 띄우지 않는다)
 * 2. 옵션이 오면 UI 가 집어갈 신호([LoginUiState.passkeyRequestJson])로 실어 보낸다
 * 3. 옵션 발급 실패는 **화면에 알리지 않는다** — 사용자가 요청한 적 없는 시도라, 기존 로그인
 *    폼이 무간섭으로 남아야 한다. 오프라인은 계측에서도 뺀다(정상 상황)
 * 4. 검증 실패는 인라인·재시도 팝업이 아니라 스낵바 하나로 모은다 — 인라인을 걸 입력 필드가
 *    없고, 재시도 팝업의 버튼은 [LoginType] 을 재실행하는 장치라 패스키에는 대상이 없다
 * 5. 검증 성공은 기존 로그인과 같은 성공 신호([LoginUiState.isLoggedIn])다
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelPasskeyTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class RecordingErrorReporter : ErrorReporter {
        val written = mutableListOf<Map<String, String>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            written += attributes
        }

        /**
         * 인증 흐름 키만 남긴 사본.
         *
         * `ErrorReporter.recordFailure` 가 예외를 redact 하면서 `error_type`(·`error_cause_type`)
         * 을 함께 붙이므로, 전체 맵을 그대로 비교하면 이 테스트가 그 정책까지 붙잡는다.
         * 여기서 보려는 것은 단계·수단 두 키뿐이다.
         */
        val authAttributes: List<Map<String, String>>
            get() = written.map { it.filterKeys { key -> key.startsWith("auth_") } }
    }

    private fun viewModel(
        passkeyRepository: FakePasskeyRepository,
        authRepository: FakeAuthRepository = FakeAuthRepository.strict().apply { onSaveSession = { _, _ -> Result.success(Unit) } },
        reporter: ErrorReporter = RecordingErrorReporter(),
    ) = LoginViewModel(
        loginUseCase = LoginUseCase(FakeAuthRepository.strict()),
        passkeyLoginUseCase =
            PasskeyLoginUseCase(passkeyRepository = passkeyRepository, authRepository = authRepository),
        errorReporter = reporter,
    )

    @Test
    fun `화면 진입 - 옵션 원문을 UI 신호로 실어 보낸다`() {
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticationOptions = { Result.success(PasskeyAuthenticationOptions("""{"challenge":"abc"}""")) }
            }
        val viewModel = viewModel(passkey)

        viewModel.startPasskeyLogin()

        assertEquals("""{"challenge":"abc"}""", viewModel.uiState.value.passkeyRequestJson)
    }

    @Test
    fun `화면 진입 - 두 번 불려도 옵션은 한 번만 받는다`() {
        // 구성 변경으로 LaunchedEffect 가 다시 도는 경우. 막지 않으면 시스템 선택기가 두 번 뜬다.
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticationOptions = { Result.success(PasskeyAuthenticationOptions("""{"challenge":"abc"}""")) }
            }
        val viewModel = viewModel(passkey)

        viewModel.startPasskeyLogin()
        viewModel.startPasskeyLogin()

        assertEquals(1, passkey.authenticationOptionsCalls)
    }

    @Test
    fun `UI 가 신호를 소비하면 비운다`() {
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticationOptions = { Result.success(PasskeyAuthenticationOptions("""{"challenge":"abc"}""")) }
            }
        val viewModel = viewModel(passkey)
        viewModel.startPasskeyLogin()

        viewModel.onPasskeyRequestConsumed()

        assertNull(viewModel.uiState.value.passkeyRequestJson)
    }

    @Test
    fun `옵션 발급 실패 - 화면에는 아무것도 알리지 않는다`() {
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticationOptions = { Result.failure(IllegalStateException("server down")) }
            }
        val viewModel = viewModel(passkey)

        viewModel.startPasskeyLogin()

        val state = viewModel.uiState.value
        assertNull(state.passkeyRequestJson)
        assertNull(state.errorMessage)
        assertFalse(state.showNetworkErrorPopup)
        assertFalse(state.hasCredentialError)
        assertFalse(state.isLoading)
    }

    @Test
    fun `옵션 발급 실패 - 오프라인은 기록하지 않고 그 밖의 실패만 기록한다`() {
        val offlineReporter = RecordingErrorReporter()
        viewModel(
            FakePasskeyRepository.strict().apply {
                onAuthenticationOptions = {
                    Result.failure(CoreAuthFailure.NetworkUnavailable(UnknownHostException("no dns")))
                }
            },
            reporter = offlineReporter,
        ).startPasskeyLogin()

        val serverReporter = RecordingErrorReporter()
        viewModel(
            FakePasskeyRepository.strict().apply {
                onAuthenticationOptions = { Result.failure(IllegalStateException("server down")) }
            },
            reporter = serverReporter,
        ).startPasskeyLogin()

        assertTrue(offlineReporter.written.isEmpty())
        assertEquals(
            mapOf("auth_stage" to "passkey_options", "auth_provider" to "passkey"),
            serverReporter.authAttributes.single(),
        )
    }

    @Test
    fun `Credential Manager 실패 - 서버 호출 이전 단계로 기록한다`() {
        val reporter = RecordingErrorReporter()
        val viewModel = viewModel(FakePasskeyRepository.strict(), reporter = reporter)

        viewModel.onPasskeyAssertionFailed(IllegalStateException("provider unavailable"))

        assertEquals(
            mapOf("auth_stage" to "passkey_assertion", "auth_provider" to "passkey"),
            reporter.authAttributes.single(),
        )
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `검증 성공 - 로그인 성공 신호를 올리고 isLoading 해제`() {
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticate = { Result.success(Session.DefaultSession("AT", "RT")) }
            }
        val auth = FakeAuthRepository.strict().apply { onSaveSession = { _, _ -> Result.success(Unit) } }
        val viewModel = viewModel(passkey, authRepository = auth)

        viewModel.loginWithPasskey("""{"id":"cid"}""")

        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("AT" to "RT", auth.saveSessionArgs)
    }

    @Test
    fun `검증 실패 - 인라인도 팝업도 아닌 스낵바 문구로 모은다`() {
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticate = { Result.failure(IllegalStateException("verification failed")) }
            }
        val viewModel = viewModel(passkey)

        viewModel.loginWithPasskey("""{"id":"cid"}""")

        val state = viewModel.uiState.value
        assertEquals(UiText.Resource(R.string.onboarding_login_passkey_failed), state.errorMessage)
        assertFalse(state.hasCredentialError)
        assertFalse(state.showNetworkErrorPopup)
        assertFalse(state.isLoading)
    }

    @Test
    fun `검증 실패 - 전송 계층 실패도 팝업을 띄우지 않는다`() {
        // 팝업의 "다시 시도하기" 는 lastAttempt(LoginType) 재실행 장치라 패스키에는 대상이 없다.
        // 띄우면 눌러도 아무 일도 일어나지 않는 버튼이 된다.
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticate = { Result.failure(CoreAuthFailure.NetworkUnavailable(UnknownHostException("no dns"))) }
            }
        val viewModel = viewModel(passkey)

        viewModel.loginWithPasskey("""{"id":"cid"}""")

        assertFalse(viewModel.uiState.value.showNetworkErrorPopup)
        assertEquals(UiText.Resource(R.string.onboarding_network_error), viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `검증 실패 - login 단계와 passkey 수단으로 기록한다`() {
        val reporter = RecordingErrorReporter()
        val passkey =
            FakePasskeyRepository.strict().apply {
                onAuthenticate = { Result.failure(IllegalStateException("verification failed")) }
            }

        viewModel(passkey, reporter = reporter).loginWithPasskey("""{"id":"cid"}""")

        assertEquals(
            mapOf("auth_stage" to "login", "auth_provider" to "passkey"),
            reporter.authAttributes.single(),
        )
    }
}
