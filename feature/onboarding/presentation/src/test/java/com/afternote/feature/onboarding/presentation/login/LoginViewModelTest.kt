package com.afternote.feature.onboarding.presentation.login

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.model.Session
import com.afternote.core.ui.UiText
import com.afternote.feature.onboarding.presentation.OnboardingFailure
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.snackbarMessage
import com.afternote.feature.onboarding.presentation.toDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

/**
 * [LoginViewModel] 실패 안내 계약 회귀 가드 (#628).
 *
 * 계약 — 자격 거절([CoreAuthFailure.InvalidLoginCredentials])은 인라인 상태
 * ([(LoginUiState.failure == OnboardingFailure.CredentialsRejected)], 입력 변경으로 해제), 전송 계층 실패
 * ([CoreAuthFailure.NetworkUnavailable])는 재시도 팝업([(LoginUiState.failure == OnboardingFailure.LoginNetworkUnavailable)]),
 * 소셜 거절([CoreAuthFailure.SocialLoginRejected])·소셜 가입 계정
 * ([CoreAuthFailure.SocialSignUpAccount])과 그 밖의 예외는 **원문을 쓰지 않고** 리소스 문구
 * 스낵바로 고정한다. 실패 시 [LoginUiState.isLoading] 을 해제한다.
 *
 * [LoginUseCase] 는 실물 사용 — Repository Result 가 VM 상태로 번역되는 경로 전체를 가드한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 이 테스트의 관심사는 표시 문구뿐이라 계측은 버린다 — 기록 계약은 [LoginViewModelReportingTest] 가 가드한다. */
    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private fun viewModel(onDefaultLogin: () -> Result<Session.DefaultSession>): LoginViewModel =
        LoginViewModel(
            loginUseCase =
                LoginUseCase(
                    FakeAuthRepository.strict().apply {
                        this.onDefaultLogin = { _, _ -> onDefaultLogin() }
                        onSaveSession = { _, _ -> Result.success(Unit) }
                    },
                ),
            errorReporter = NoopErrorReporter,
        )

    private fun LoginViewModel.attemptEmailLogin() {
        updateEmail("user@example.com")
        updatePassword("pw")
        loginWithEmail()
    }

    @Test
    fun `전송 계층 실패 - 재시도 팝업 상태로 표시하고 isLoading 해제`() {
        val viewModel =
            viewModel(onDefaultLogin = {
                Result.failure(CoreAuthFailure.NetworkUnavailable(UnknownHostException("Unable to resolve host")))
            })

        viewModel.attemptEmailLogin()

        val state = viewModel.uiState.value
        assertTrue((state.failure == OnboardingFailure.LoginNetworkUnavailable))
        assertNull(state.failure.toDisplay().snackbarMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `retryLogin - 팝업을 닫고 마지막 시도를 같은 자격으로 재실행`() {
        var attempts = 0
        val viewModel =
            viewModel(onDefaultLogin = {
                attempts++
                Result.failure(CoreAuthFailure.NetworkUnavailable(UnknownHostException("Unable to resolve host")))
            })
        viewModel.attemptEmailLogin()

        viewModel.retryLogin()

        assertEquals(2, attempts)
    }

    @Test
    fun `onNetworkErrorDismissed - 재실행 없이 팝업만 닫음`() {
        var attempts = 0
        val viewModel =
            viewModel(onDefaultLogin = {
                attempts++
                Result.failure(CoreAuthFailure.NetworkUnavailable(UnknownHostException("Unable to resolve host")))
            })
        viewModel.attemptEmailLogin()

        viewModel.onNetworkErrorDismissed()

        assertFalse((viewModel.uiState.value.failure == OnboardingFailure.LoginNetworkUnavailable))
        assertEquals(1, attempts)
    }

    @Test
    fun `자격 거절 - 인라인 상태로 표시하고 스낵바는 비움`() {
        val viewModel =
            viewModel(onDefaultLogin = {
                Result.failure(CoreAuthFailure.InvalidLoginCredentials(Exception("origin")))
            })

        viewModel.attemptEmailLogin()

        val state = viewModel.uiState.value
        assertTrue((state.failure == OnboardingFailure.CredentialsRejected))
        assertNull(state.failure.toDisplay().snackbarMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `자격 거절 - 입력이 바뀌면 인라인 표시 해제`() {
        val viewModel =
            viewModel(onDefaultLogin = {
                Result.failure(CoreAuthFailure.InvalidLoginCredentials(Exception("origin")))
            })
        viewModel.attemptEmailLogin()

        viewModel.updatePassword("new-pw")

        assertFalse((viewModel.uiState.value.failure == OnboardingFailure.CredentialsRejected))
    }

    @Test
    fun `소셜 거절 - 서버 문구 대신 자체 리소스 문구로 표시`() {
        val viewModel =
            viewModel(onDefaultLogin = {
                Result.failure(CoreAuthFailure.SocialLoginRejected(Exception("origin")))
            })

        viewModel.attemptEmailLogin()

        assertEquals(
            UiText.Resource(R.string.onboarding_login_social_rejected),
            viewModel.uiState.value.failure
                .toDisplay()
                .snackbarMessage,
        )
    }

    @Test
    fun `소셜 가입 계정 - 자격 인라인이 아니라 로그인 화면 전용 안내로 표시`() {
        val viewModel =
            viewModel(onDefaultLogin = {
                Result.failure(CoreAuthFailure.SocialSignUpAccount(Exception("1702")))
            })

        viewModel.attemptEmailLogin()

        val state = viewModel.uiState.value
        assertEquals(UiText.Resource(R.string.onboarding_login_social_signup_account), state.failure.toDisplay().snackbarMessage)
        // 비밀번호 찾기 쪽 차단 문구를 돌려쓰지 않는다 — 로그인 화면에서는 틀린 안내다.
        assertNotEquals(UiText.Resource(R.string.onboarding_find_password_social_blocked), state.failure.toDisplay().snackbarMessage)
        assertFalse((state.failure == OnboardingFailure.CredentialsRejected))
        assertFalse(state.isLoading)
    }

    @Test
    fun `그 외 실패 - 예외 원문 대신 일반 문구로 고정 (5xx 본문·역직렬화 원문 차단)`() {
        val internalMessage = "ERROR: duplicate key value violates unique constraint"
        val viewModel = viewModel(onDefaultLogin = { Result.failure(Exception(internalMessage)) })

        viewModel.attemptEmailLogin()

        val state = viewModel.uiState.value
        assertEquals(UiText.Resource(R.string.onboarding_login_failed), state.failure.toDisplay().snackbarMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `message 가 없는 실패 - 안내가 소실되지 않고 일반 문구로 표시 (무음 방지)`() {
        val viewModel = viewModel(onDefaultLogin = { Result.failure(Exception()) })

        viewModel.attemptEmailLogin()

        assertEquals(
            UiText.Resource(R.string.onboarding_login_failed),
            viewModel.uiState.value.failure
                .toDisplay()
                .snackbarMessage,
        )
    }

    @Test
    fun `onErrorConsumed - 표시 후 리셋되어 재노출되지 않음`() {
        val viewModel = viewModel(onDefaultLogin = { Result.failure(Exception("실패")) })
        viewModel.attemptEmailLogin()

        viewModel.onErrorConsumed()

        assertNull(
            viewModel.uiState.value.failure
                .toDisplay()
                .snackbarMessage,
        )
    }

    @Test
    fun `새 자격 거절은 미소비 스낵바를 대체하고 늦은 스낵바 소비로 지워지지 않는다`() {
        var failure: Throwable = IllegalStateException("first request failed")
        val viewModel = viewModel { Result.failure(failure) }
        viewModel.attemptEmailLogin()
        assertTrue(viewModel.uiState.value.failure is OnboardingFailure.RequestFailed)

        failure = CoreAuthFailure.InvalidLoginCredentials(Exception("invalid credentials"))
        viewModel.loginWithEmail()
        viewModel.onErrorConsumed()

        assertEquals(OnboardingFailure.CredentialsRejected, viewModel.uiState.value.failure)
        assertNull(
            viewModel.uiState.value.failure
                .toDisplay()
                .snackbarMessage,
        )
    }

    @Test
    fun `화면의 같은 입력 재전달은 실패를 남기고 실제 입력 변경은 실패를 지운다`() {
        val viewModel = viewModel { Result.failure(IllegalStateException("failed")) }
        viewModel.attemptEmailLogin()

        viewModel.updateEmail("user@example.com")
        assertTrue(viewModel.uiState.value.failure is OnboardingFailure.RequestFailed)
        viewModel.updateEmail("other@example.com")
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `늦은 네트워크 팝업 닫기는 새 요청 실패를 지우지 않는다`() {
        val viewModel = viewModel { Result.failure(IllegalStateException("failed")) }
        viewModel.attemptEmailLogin()
        viewModel.onNetworkErrorDismissed()
        assertTrue(viewModel.uiState.value.failure is OnboardingFailure.RequestFailed)
    }
}
