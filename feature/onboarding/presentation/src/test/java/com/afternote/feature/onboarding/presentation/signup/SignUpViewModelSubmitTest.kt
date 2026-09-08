package com.afternote.feature.onboarding.presentation.signup

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.core.model.Session
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [SignUpViewModel.submitSignUp] 제출 잠금·부분 성공 복구 회귀 가드 (#710).
 *
 * 상태 플래그만으로는 연타를 막지 못한다 — 버튼과 IME 두 경로가 모두 `isLoading = false` 를
 * 읽고 통과할 수 있다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelSubmitTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private fun viewModel(
        accountRepository: AccountRepository,
        onDefaultLogin: () -> Result<Session.DefaultSession> = { Result.success(Session.DefaultSession("access", "refresh")) },
    ): SignUpViewModel =
        SignUpViewModel(
            accountRepository = accountRepository,
            loginUseCase =
                LoginUseCase(
                    FakeAuthRepository.strict().apply {
                        this.onDefaultLogin = { _, _ -> onDefaultLogin() }
                        onSaveSession = { _, _ -> Result.success(Unit) }
                    },
                ),
            errorReporter = NoopErrorReporter,
        ).apply {
            updateEmail("user@example.com")
            updateSignUpPassword("Password1!")
            updateName("애프터노트")
        }

    @Test
    fun `제출이 진행 중이면 재호출이 가입을 다시 부르지 않는다`() =
        runTest {
            // 첫 제출을 signUp 안에서 붙잡아 두고 그 사이 두 번째 제출을 시도한다 — 연타·IME 중복 경로.
            val inFlight = CompletableDeferred<Unit>()
            val repository =
                FakeAccountRepository(
                    onSignUp = {
                        inFlight.await()
                        Result.success(AccountRegistration(userId = 1L, email = "user@example.com"))
                    },
                )
            val viewModel = viewModel(repository)

            viewModel.submitSignUp()
            viewModel.submitSignUp()
            viewModel.submitSignUp()

            assertEquals(1, repository.signUpCallCount)
            assertTrue(viewModel.uiState.value.isLoading)

            inFlight.complete(Unit)

            assertTrue(viewModel.uiState.value.isSignedUp)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(1, repository.signUpCallCount)
        }

    @Test
    fun `자동 로그인만 실패하면 재제출이 가입을 건너뛰고 로그인만 다시 한다`() {
        val repository =
            FakeAccountRepository(
                onSignUp = { Result.success(AccountRegistration(userId = 1L, email = "user@example.com")) },
            )
        var loginAttempts = 0
        val viewModel =
            viewModel(repository) {
                loginAttempts++
                if (loginAttempts == 1) {
                    Result.failure(Exception("자동 로그인 실패"))
                } else {
                    Result.success(Session.DefaultSession("access", "refresh"))
                }
            }

        viewModel.submitSignUp()

        assertEquals(1, repository.signUpCallCount)
        assertTrue(viewModel.uiState.value.isAccountCreated)
        assertFalse(viewModel.uiState.value.isSignedUp)

        viewModel.submitSignUp()

        assertEquals(1, repository.signUpCallCount)
        assertEquals(2, loginAttempts)
        assertTrue(viewModel.uiState.value.isSignedUp)
    }

    @Test
    fun `이름이 공백이면 요청 없이 안내만 세운다`() {
        val repository = FakeAccountRepository(onSignUp = { error("이름 미입력이면 호출되면 안 됨") })
        val viewModel = viewModel(repository).apply { updateName("   ") }

        viewModel.submitSignUp()

        assertEquals(0, repository.signUpCallCount)
        assertTrue(viewModel.uiState.value.isNameRequired)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

/** 미지정 경로 호출은 error 로 드러낸다 (core:data 의 Fake 들과 같은 규칙). */
private class FakeAccountRepository(
    private val onSignUp: suspend () -> Result<AccountRegistration>,
) : AccountRepository {
    var signUpCallCount = 0
        private set

    override suspend fun signUp(
        email: String,
        password: String,
        name: String,
        profileUrl: String?,
    ): Result<AccountRegistration> {
        signUpCallCount++
        return onSignUp()
    }

    override suspend fun sendEmailCode(email: String): Result<Unit> = error("sendEmailCode 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun verifyEmail(
        email: String,
        certificateCode: String,
    ): Result<Unit> = error("verifyEmail 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun sendFindCode(email: String): Result<Unit> = error("sendFindCode 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun findAccount(
        email: String,
        certificateCode: String,
    ): Result<FoundAccount> = error("findAccount 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun resetPassword(
        email: String,
        certificateCode: String,
        newPassword: String,
        confirmPassword: String,
    ): Result<Unit> = error("resetPassword 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun passwordChange(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit> = error("passwordChange 는 이 시나리오에서 호출되면 안 됨")
}
