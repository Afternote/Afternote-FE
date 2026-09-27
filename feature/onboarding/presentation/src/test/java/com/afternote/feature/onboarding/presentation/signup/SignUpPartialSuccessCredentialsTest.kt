package com.afternote.feature.onboarding.presentation.signup

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.core.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 부분 성공 복구가 **실제로 만들어진 계정에만** 적용되는지 (#2026).
 *
 * 회원가입 POST 는 성공했는데 자동 로그인만 실패하면 화면은 그대로 남는다. #710 이 그 재제출에서
 * 가입을 다시 부르지 않도록 막았는데, 그 판정이 Boolean 하나였다 — **부분 성공 뒤 뒤로 가
 * 이메일·비밀번호를 고쳐도 그 플래그가 새 입력에 그대로 남는다.** 그러면 새 계정의 가입은
 * 건너뛰고 새 자격으로 로그인만 불러, 만들어진 적 없는 계정으로 복구가 돈다.
 *
 * 두 축을 함께 본다 — **자격을 바꾸면 새로 가입해야 하고, 바꾸지 않으면 #710 대로 건너뛰어야 한다.**
 * 한쪽만 보면 중복 가입을 막다가 새 계정을 못 만들거나, 그 반대가 된다.
 *
 * Robolectric 은 [SignUpUiState] 의 이메일 형식 검사가 `android.util.Patterns` 를 읽기 때문에만 필요하다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SignUpPartialSuccessCredentialsTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `부분 성공 뒤 이메일을 바꾸면 새 계정으로 다시 가입한다`() =
        runTest {
            val repository = RecordingAccountRepository()
            val viewModel = viewModel(repository, loginFails = true)
            viewModel.onIntent(SignUpIntent.SubmitSignUp)

            assertEquals(listOf(OLD_EMAIL), repository.signedUpEmails)

            viewModel.onIntent(SignUpIntent.UpdateEmail(NEW_EMAIL))
            viewModel.onIntent(SignUpIntent.SubmitSignUp)

            assertEquals(
                "만들어진 적 없는 계정으로 복구가 돌았다",
                listOf(OLD_EMAIL, NEW_EMAIL),
                repository.signedUpEmails,
            )
        }

    @Test
    fun `부분 성공 뒤 비밀번호만 바꿔도 새 계정으로 다시 가입한다`() =
        runTest {
            val repository = RecordingAccountRepository()
            val viewModel = viewModel(repository, loginFails = true)
            viewModel.onIntent(SignUpIntent.SubmitSignUp)

            viewModel.onIntent(SignUpIntent.UpdateSignUpPassword("Changed1!"))
            viewModel.onIntent(SignUpIntent.SubmitSignUp)

            assertEquals(listOf(OLD_EMAIL, OLD_EMAIL), repository.signedUpEmails)
            assertEquals(
                "생성 때 비밀번호와 재시도 비밀번호가 어긋난 채 가입을 건너뛰었다",
                listOf(PASSWORD, "Changed1!"),
                repository.signedUpPasswords,
            )
        }

    /** #710 의 정상 복구 — 자격을 그대로 두고 재제출하면 가입을 다시 부르지 않는다. */
    @Test
    fun `자격을 바꾸지 않은 재제출은 가입을 다시 부르지 않는다`() =
        runTest {
            val repository = RecordingAccountRepository()
            val viewModel = viewModel(repository, loginFails = true)
            viewModel.onIntent(SignUpIntent.SubmitSignUp)
            viewModel.onIntent(SignUpIntent.SubmitSignUp)

            assertEquals(listOf(OLD_EMAIL), repository.signedUpEmails)
        }

    private fun viewModel(
        repository: AccountRepository,
        loginFails: Boolean,
    ): SignUpViewModel =
        SignUpViewModel(
            accountRepository = repository,
            loginUseCase =
                LoginUseCase(
                    FakeAuthRepository.strict().apply {
                        onDefaultLogin = { _, _ ->
                            if (loginFails) {
                                Result.failure(IOException("자동 로그인 실패"))
                            } else {
                                Result.success(Session.DefaultSession("access", "refresh"))
                            }
                        }
                        onSaveSession = { _, _ -> Result.success(Unit) }
                    },
                ),
            errorReporter = NoopErrorReporter,
        ).apply {
            onIntent(SignUpIntent.UpdateEmail(OLD_EMAIL))
            onIntent(SignUpIntent.UpdateSignUpPassword(PASSWORD))
            onIntent(SignUpIntent.UpdateName("애프터노트"))
        }

    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private class RecordingAccountRepository : AccountRepository {
        val signedUpEmails = mutableListOf<String>()
        val signedUpPasswords = mutableListOf<String>()

        override suspend fun signUp(
            email: String,
            password: String,
            name: String,
            profileUrl: String?,
        ): Result<AccountRegistration> {
            signedUpEmails += email
            signedUpPasswords += password
            return Result.success(AccountRegistration(userId = 1L, email = email))
        }

        override suspend fun sendEmailCode(email: String): Result<Unit> = error("이 테스트가 부르지 않는 경로")

        override suspend fun verifyEmail(
            email: String,
            certificateCode: String,
        ): Result<Unit> = error("이 테스트가 부르지 않는 경로")

        override suspend fun sendFindCode(email: String): Result<Unit> = error("이 테스트가 부르지 않는 경로")

        override suspend fun findAccount(
            email: String,
            certificateCode: String,
        ): Result<FoundAccount> = error("이 테스트가 부르지 않는 경로")

        override suspend fun resetPassword(
            email: String,
            certificateCode: String,
            newPassword: String,
            confirmPassword: String,
        ): Result<Unit> = error("이 테스트가 부르지 않는 경로")

        override suspend fun passwordChange(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> = error("이 테스트가 부르지 않는 경로")
    }

    private companion object {
        const val OLD_EMAIL = "old@example.com"
        const val NEW_EMAIL = "new@example.com"
        const val PASSWORD = "Password1!"
    }
}
