package com.afternote.feature.onboarding.presentation.signup

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.feature.onboarding.presentation.OnboardingFailure
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 인증 결과가 **자기가 검증한 입력에만 적용되는지** (#2025).
 *
 * 1단계의 이메일 칸은 인증이 도는 동안에도 고칠 수 있다(잠기는 것은 하단 제출 버튼뿐이다).
 * 결과에 「무엇을 검증했는지」가 없으면 리듀서가 지금 화면의 답과 버린 시도의 답을 가르지 못해,
 * A 의 성공이 B 를 적은 폼을 다음 단계로 밀어 버린다 — **서버가 검증한 적 없는 이메일로 가입이
 * 이어진다.** 실패도 같다: 지운 입력의 거절 문구가 새 입력 아래에 붙는다.
 *
 * 새 요청의 상태까지 지우지는 않는다는 것도 함께 본다 — 옛 결과가 왔다는 이유로 지금 폼을
 * 초기화하면 고친 쪽이 지워진다.
 *
 * Robolectric 은 [SignUpUiState] 의 이메일 형식 검사가 `android.util.Patterns` 를 읽기 때문에만 필요하다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SignUpStaleVerificationTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `이메일을 바꾼 뒤 도착한 옛 인증 성공은 다음 단계로 보내지 않는다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val repository =
                StubAccountRepository {
                    gate.await()
                    Result.success(Unit)
                }
            val viewModel = viewModel(repository)

            viewModel.onIntent(SignUpIntent.VerifyEmailAndProceed)
            viewModel.onIntent(SignUpIntent.UpdateEmail(NEW_EMAIL))
            gate.complete(Unit)

            val state = viewModel.uiState.value
            assertEquals(NEW_EMAIL, state.email)
            assertFalse("검증한 적 없는 이메일로 다음 단계가 열렸다", state.shouldNavigateToResidentNumber)
            assertEquals(OLD_EMAIL, repository.verifiedEmails.single())
        }

    @Test
    fun `인증번호를 바꾼 뒤 도착한 옛 인증 성공도 다음 단계로 보내지 않는다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val repository =
                StubAccountRepository {
                    gate.await()
                    Result.success(Unit)
                }
            val viewModel = viewModel(repository)

            viewModel.onIntent(SignUpIntent.VerifyEmailAndProceed)
            viewModel.onIntent(SignUpIntent.UpdateVerificationCode("999999"))
            gate.complete(Unit)

            assertFalse(viewModel.uiState.value.shouldNavigateToResidentNumber)
        }

    @Test
    fun `이메일을 바꾼 뒤 도착한 옛 인증 거절은 새 입력에 오류를 붙이지 않는다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val repository =
                StubAccountRepository {
                    gate.await()
                    Result.failure(CoreAuthFailure.EmailVerification(IOException("1207")))
                }
            val viewModel = viewModel(repository)

            viewModel.onIntent(SignUpIntent.VerifyEmailAndProceed)
            viewModel.onIntent(SignUpIntent.UpdateEmail(NEW_EMAIL))
            gate.complete(Unit)

            assertNull("버린 시도의 거절이 새 입력에 붙었다", viewModel.uiState.value.failure)
        }

    @Test
    fun `이메일을 바꾼 뒤 도착한 옛 요청 실패도 새 입력에 오류를 붙이지 않는다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val repository =
                StubAccountRepository {
                    gate.await()
                    Result.failure(IOException("offline"))
                }
            val viewModel = viewModel(repository)

            viewModel.onIntent(SignUpIntent.VerifyEmailAndProceed)
            viewModel.onIntent(SignUpIntent.UpdateEmail(NEW_EMAIL))
            gate.complete(Unit)

            assertNull(viewModel.uiState.value.failure)
        }

    /** 입력을 그대로 둔 정상 경로는 종전과 같다. */
    @Test
    fun `입력을 바꾸지 않았으면 인증 성공이 다음 단계를 연다`() =
        runTest {
            val repository = StubAccountRepository { Result.success(Unit) }
            val viewModel = viewModel(repository)

            viewModel.onIntent(SignUpIntent.VerifyEmailAndProceed)

            assertTrue(viewModel.uiState.value.shouldNavigateToResidentNumber)
        }

    /** 정상 경로의 거절도 종전과 같이 인라인 문구로 남는다. */
    @Test
    fun `입력을 바꾸지 않았으면 인증 거절이 그대로 표시된다`() =
        runTest {
            val repository =
                StubAccountRepository { Result.failure(CoreAuthFailure.EmailVerification(IOException("1207"))) }
            val viewModel = viewModel(repository)

            viewModel.onIntent(SignUpIntent.VerifyEmailAndProceed)

            assertEquals(OnboardingFailure.VerificationRejected, viewModel.uiState.value.failure)
        }

    /** 옛 결과가 왔다는 이유로 진행 중 표시까지 굳어 있으면 새 요청을 걸 수 없다. */
    @Test
    fun `옛 결과가 도착하면 진행 중 표시는 풀린다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val repository =
                StubAccountRepository {
                    gate.await()
                    Result.success(Unit)
                }
            val viewModel = viewModel(repository)

            viewModel.onIntent(SignUpIntent.VerifyEmailAndProceed)
            viewModel.onIntent(SignUpIntent.UpdateEmail(NEW_EMAIL))
            gate.complete(Unit)

            assertFalse("진행 중 표시가 굳어 새 인증을 걸 수 없다", viewModel.uiState.value.isVerifyingEmail)
        }

    private fun viewModel(repository: AccountRepository): SignUpViewModel =
        SignUpViewModel(
            accountRepository = repository,
            loginUseCase =
                LoginUseCase(
                    FakeAuthRepository.strict().apply {
                        onSaveSession = { _, _ -> Result.success(Unit) }
                    },
                ),
            errorReporter = NoopErrorReporter,
        ).apply {
            onIntent(SignUpIntent.UpdateEmail(OLD_EMAIL))
            onIntent(SignUpIntent.UpdateVerificationCode(CODE))
        }

    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private class StubAccountRepository(
        private val onVerifyEmail: suspend () -> Result<Unit>,
    ) : AccountRepository {
        val verifiedEmails = mutableListOf<String>()

        override suspend fun verifyEmail(
            email: String,
            certificateCode: String,
        ): Result<Unit> {
            verifiedEmails += email
            return onVerifyEmail()
        }

        override suspend fun sendEmailCode(email: String): Result<Unit> = error("이 테스트가 부르지 않는 경로")

        override suspend fun sendFindCode(email: String): Result<Unit> = error("이 테스트가 부르지 않는 경로")

        override suspend fun findAccount(
            email: String,
            certificateCode: String,
        ): Result<FoundAccount> = error("이 테스트가 부르지 않는 경로")

        override suspend fun signUp(
            email: String,
            password: String,
            name: String,
            profileUrl: String?,
        ): Result<AccountRegistration> = error("이 테스트가 부르지 않는 경로")

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
        const val CODE = "123456"
    }
}
