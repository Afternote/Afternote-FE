package com.afternote.feature.onboarding.presentation.login

import com.afternote.core.domain.error.LoginRejectedException
import com.afternote.core.domain.error.NetworkUnavailableException
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.ui.UiText
import com.afternote.feature.onboarding.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

/**
 * [LoginViewModel] 실패 안내 계약 회귀 가드 (#517, PR #647 리뷰 반영).
 *
 * 계약 — 전송 계층 실패([NetworkUnavailableException])는 네트워크 안내 리소스로, 사유가 확인된
 * 거절([LoginRejectedException])은 서버 문구로 표시하고, **그 밖의 예외는 원문을 쓰지 않고**
 * 일반 문구로 고정한다. 실패 시 [LoginUiState.isLoading] 을 해제하고
 * [LoginViewModel.onErrorConsumed] 가 안내를 리셋한다.
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

    private fun viewModel(onDefaultLogin: () -> Result<Session.DefaultSession>): LoginViewModel =
        LoginViewModel(LoginUseCase(FakeAuthRepository(onDefaultLogin)))

    private fun LoginViewModel.attemptEmailLogin() {
        updateEmail("user@example.com")
        updatePassword("pw")
        loginWithEmail()
    }

    @Test
    fun `전송 계층 실패 - 네트워크 안내 리소스로 고정하고 isLoading 해제`() {
        val viewModel =
            viewModel(onDefaultLogin = {
                Result.failure(NetworkUnavailableException(UnknownHostException("Unable to resolve host")))
            })

        viewModel.attemptEmailLogin()

        val state = viewModel.uiState.value
        assertEquals(UiText.Resource(R.string.login_network_error), state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `사유 확인된 거절 - 서버 문구를 표시값으로 운반하고 isLoading 해제`() {
        val serverMessage = "아이디 또는 비밀번호가 일치하지 않습니다."
        val viewModel =
            viewModel(onDefaultLogin = {
                Result.failure(LoginRejectedException(serverMessage, Exception("origin")))
            })

        viewModel.attemptEmailLogin()

        val state = viewModel.uiState.value
        assertEquals(UiText.Dynamic(serverMessage), state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `그 외 실패 - 예외 원문 대신 일반 문구로 고정 (5xx 본문·역직렬화 원문 차단)`() {
        val internalMessage = "ERROR: duplicate key value violates unique constraint"
        val viewModel = viewModel(onDefaultLogin = { Result.failure(Exception(internalMessage)) })

        viewModel.attemptEmailLogin()

        val state = viewModel.uiState.value
        assertEquals(UiText.Resource(R.string.login_failed), state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `message 가 없는 실패 - 안내가 소실되지 않고 일반 문구로 표시 (무음 방지)`() {
        val viewModel = viewModel(onDefaultLogin = { Result.failure(Exception()) })

        viewModel.attemptEmailLogin()

        assertEquals(UiText.Resource(R.string.login_failed), viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onErrorConsumed - 표시 후 리셋되어 재노출되지 않음`() {
        val viewModel = viewModel(onDefaultLogin = { Result.failure(Exception("실패")) })
        viewModel.attemptEmailLogin()

        viewModel.onErrorConsumed()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}

/**
 * [AuthRepository] 테스트 공용 가짜 — 미지정 경로 호출은 error 로 드러낸다
 * (core:data 의 FakeAuthApiService 와 같은 규칙). 로그인 성공 뒤 세션 저장까지가
 * [LoginUseCase] 경로라 [saveSession] 만 성공 고정으로 열어 둔다.
 */
private class FakeAuthRepository(
    private val onDefaultLogin: () -> Result<Session.DefaultSession>,
) : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = flowOf(false)

    override suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = error("updateTokens 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun clearSession(): Result<Unit> = error("clearSession 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun getAccessToken(): Result<String?> = error("getAccessToken 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun getRefreshToken(): Result<String?> = error("getRefreshToken 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession> = onDefaultLogin()

    override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> = error("kakaoLogin 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> = error("googleLogin 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun rotateToken(): Result<TokenBundle> = error("rotateToken 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun logout(): Result<Unit> = error("logout 은 이 시나리오에서 호출되면 안 됨")
}
