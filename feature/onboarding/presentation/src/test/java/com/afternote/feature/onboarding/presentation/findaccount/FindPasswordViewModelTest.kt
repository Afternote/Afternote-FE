package com.afternote.feature.onboarding.presentation.findaccount

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.core.ui.UiText
import com.afternote.feature.onboarding.presentation.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
 * 비밀번호 찾기 흐름의 상태 전이 회귀 가드 (#457).
 *
 * 가드하는 계약 네 가지.
 * 1. 소셜 가입 계정(서버 code 1702)은 **스낵바가 아니라 차단 팝업** 신호로 갈린다 — 시안 `2383:16667`.
 * 2. 인증번호를 서버에 미리 확인하지 않는다 — 확인하면 서버가 코드를 지워 최종 제출이 실패한다.
 * 3. 최종 제출의 1207 은 전용 문구로 갈린다 — 폴백 문구로는 "무엇을 다시 해야 하는지" 를 못 말한다.
 * 4. 완료 후 흐름 상태를 버린다 — 그래프 스코프 VM 이라 재진입 시 완료 화면으로 튀고,
 *    평문 비밀번호가 계속 남는다.
 *
 * [RobolectricTestRunner] 를 쓰는 이유는 [FindPasswordUiState.isEmailFormatValid] 가
 * `android.util.Patterns` 를 읽기 때문이다(JVM 단독 실행에서는 stub 이 던진다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FindPasswordViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `소셜 가입 계정은 스낵바가 아니라 차단 팝업 신호로 갈린다`() {
        val repository = FakeAccountRepository(onSendFindCode = { Result.failure(socialSignUpAccount()) })
        val reporter = RecordingErrorReporter()
        val viewModel = viewModel(repository, reporter).apply { updateEmail(EMAIL) }

        viewModel.requestVerificationCode()

        val state = viewModel.uiState.value
        assertTrue(state.isSocialSignUpAccount)
        assertNull(state.errorMessage)
        assertFalse(state.isVerificationSent)
        // 서버가 정상적으로 가르는 분기라 장애로 세지 않는다.
        assertEquals(0, reporter.recordedCount)
    }

    @Test
    fun `발송 실패 중 소셜 계정이 아닌 사유는 스낵바로 가고 계측된다`() {
        val repository = FakeAccountRepository(onSendFindCode = { Result.failure(CoreAuthFailure.NetworkUnavailable(IOException())) })
        val reporter = RecordingErrorReporter()
        val viewModel = viewModel(repository, reporter).apply { updateEmail(EMAIL) }

        viewModel.requestVerificationCode()

        val state = viewModel.uiState.value
        assertFalse(state.isSocialSignUpAccount)
        assertEquals(UiText.Resource(R.string.onboarding_network_error), state.errorMessage)
        assertEquals(1, reporter.recordedCount)
    }

    @Test
    fun `이메일을 고치면 앞선 발송 이력과 차단 판정이 사라진다`() {
        val repository = FakeAccountRepository(onSendFindCode = { Result.failure(socialSignUpAccount()) })
        val viewModel = viewModel(repository).apply { updateEmail(EMAIL) }
        viewModel.requestVerificationCode()

        viewModel.updateEmail("other@example.com")

        val state = viewModel.uiState.value
        assertFalse(state.isSocialSignUpAccount)
        assertFalse(state.isVerificationSent)
    }

    @Test
    fun `인증 화면의 다음은 서버에 코드를 확인하지 않는다`() {
        // findAccount(=auth/email/find)를 부르면 서버가 인증번호를 지워 최종 제출이 실패한다.
        val repository = FakeAccountRepository(onSendFindCode = { Result.success(Unit) })
        val viewModel =
            viewModel(repository).apply {
                updateEmail(EMAIL)
                requestVerificationCode()
                updateCertificateCode(CODE)
            }

        assertTrue(viewModel.uiState.value.isVerificationNextEnabled)
        assertEquals(0, repository.findAccountCallCount)
    }

    @Test
    fun `발송 전에는 자릿수를 채워도 다음이 열리지 않는다`() {
        val repository = FakeAccountRepository()
        val viewModel =
            viewModel(repository).apply {
                updateEmail(EMAIL)
                updateCertificateCode(CODE)
            }

        assertFalse(viewModel.uiState.value.isVerificationNextEnabled)
    }

    @Test
    fun `비밀번호 확인이 다르면 제출하지 않는다`() {
        val repository = FakeAccountRepository()
        val viewModel =
            readyToSubmit(repository).apply {
                updateNewPasswordConfirm("Different1!")
            }

        viewModel.submitNewPassword()

        assertEquals(0, repository.resetPasswordCallCount)
    }

    @Test
    fun `제출이 진행 중이면 재호출이 재설정을 다시 부르지 않는다`() =
        runTest {
            val inFlight = CompletableDeferred<Unit>()
            val repository =
                FakeAccountRepository(
                    onResetPassword = {
                        inFlight.await()
                        Result.success(Unit)
                    },
                )
            val viewModel = readyToSubmit(repository)

            viewModel.submitNewPassword()
            viewModel.submitNewPassword()

            assertEquals(1, repository.resetPasswordCallCount)
            assertTrue(viewModel.uiState.value.isSubmitting)

            inFlight.complete(Unit)

            assertTrue(viewModel.uiState.value.isPasswordChanged)
            assertFalse(viewModel.uiState.value.isSubmitting)
            assertEquals(1, repository.resetPasswordCallCount)
        }

    /**
     * 바로 위 테스트와 **다른 창**을 본다 — 저쪽은 `isSubmitting` 이 상태에 반영된 뒤의 재호출이라
     * `isResetEnabled` 하나로 막히지만, 이쪽은 반영 전이다.
     *
     * 실기기의 `Dispatchers.Main` 은 `launch` 본문을 다음 루프에 돌리므로 빠른 두 탭이 모두
     * `isSubmitting = false` 를 읽고 통과할 수 있다. [StandardTestDispatcher] 가 그 창을 그대로
     * 재현한다 — 상태 플래그만으로는 여기서 못 막고 진행 중인 Job 을 봐야 한다.
     *
     * 이 흐름에서 요청이 두 번 나가면 낭비로 끝나지 않는다. 서버가 인증번호를 검증하며 지우므로
     * 두 번째가 1207 로 실패해, 첫 요청이 성공했는데도 "이메일 인증부터 다시" 안내가 뜬다.
     */
    @Test
    fun `제출 연타는 상태가 반영되기 전에도 재설정을 한 번만 부른다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeAccountRepository(onResetPassword = { Result.success(Unit) })
            val viewModel = readyToSubmit(repository)

            viewModel.submitNewPassword()
            viewModel.submitNewPassword()
            advanceUntilIdle()

            assertEquals(1, repository.resetPasswordCallCount)
        }

    /** 발송도 같은 창을 갖는다 — 중복 발송은 앞서 안내한 인증번호를 무효로 만든다. */
    @Test
    fun `인증번호 발송 연타는 상태가 반영되기 전에도 요청을 한 번만 낸다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            var sendCalls = 0
            val repository =
                FakeAccountRepository(
                    onSendFindCode = {
                        sendCalls++
                        Result.success(Unit)
                    },
                )
            val viewModel = viewModel(repository).apply { updateEmail(EMAIL) }

            viewModel.requestVerificationCode()
            viewModel.requestVerificationCode()
            advanceUntilIdle()

            assertEquals(1, sendCalls)
        }

    @Test
    fun `제출은 인증 화면에서 받은 인증번호를 그대로 싣는다`() {
        val repository = FakeAccountRepository(onResetPassword = { Result.success(Unit) })
        val viewModel = readyToSubmit(repository)

        viewModel.submitNewPassword()

        assertEquals(
            ResetPasswordCall(EMAIL, CODE, PASSWORD, PASSWORD),
            repository.lastResetPasswordCall,
        )
    }

    @Test
    fun `제출 시점의 인증번호 무효는 전용 문구로 갈린다`() {
        val repository = FakeAccountRepository(onResetPassword = { Result.failure(emailVerification()) })
        val viewModel = readyToSubmit(repository)

        viewModel.submitNewPassword()

        assertEquals(
            UiText.Resource(R.string.onboarding_find_password_code_expired),
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.isPasswordChanged)
    }

    @Test
    fun `사유를 확인하지 못한 제출 실패는 재설정 폴백 문구로 내려앉는다`() {
        val repository = FakeAccountRepository(onResetPassword = { Result.failure(IllegalStateException("boom")) })
        val viewModel = readyToSubmit(repository)

        viewModel.submitNewPassword()

        assertEquals(
            UiText.Resource(R.string.onboarding_find_password_failed),
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `완료 신호를 소비하면 인증번호와 비밀번호가 상태에서 사라진다`() {
        val repository = FakeAccountRepository(onResetPassword = { Result.success(Unit) })
        val viewModel = readyToSubmit(repository)
        viewModel.submitNewPassword()

        viewModel.onPasswordResetConsumed()

        assertEquals(FindPasswordUiState(), viewModel.uiState.value)
    }

    private fun viewModel(
        repository: AccountRepository,
        errorReporter: ErrorReporter = RecordingErrorReporter(),
    ) = FindPasswordViewModel(accountRepository = repository, errorReporter = errorReporter)

    /** 인증 화면을 통과해 비밀번호까지 채운 상태 — 제출 직전. */
    private fun readyToSubmit(repository: FakeAccountRepository) =
        viewModel(repository).apply {
            updateEmail(EMAIL)
            updateCertificateCode(CODE)
            updateNewPassword(PASSWORD)
            updateNewPasswordConfirm(PASSWORD)
        }

    private fun socialSignUpAccount() = CoreAuthFailure.SocialSignUpAccount(IllegalStateException("1702"))

    private fun emailVerification() = CoreAuthFailure.EmailVerification(IllegalStateException("1207"))

    private companion object {
        const val EMAIL = "user@example.com"
        const val CODE = "123456"
        const val PASSWORD = "NewPass1!"
    }
}

private data class ResetPasswordCall(
    val email: String,
    val certificateCode: String,
    val newPassword: String,
    val confirmPassword: String,
)

private class RecordingErrorReporter : ErrorReporter {
    var recordedCount = 0
        private set

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        recordedCount++
    }
}

/** 미지정 경로 호출은 error 로 드러낸다 (core:data 의 Fake 들과 같은 규칙). */
private class FakeAccountRepository(
    private val onSendFindCode: suspend () -> Result<Unit> = { error("sendFindCode 는 이 시나리오에서 호출되면 안 됨") },
    private val onResetPassword: suspend () -> Result<Unit> = { error("resetPassword 는 이 시나리오에서 호출되면 안 됨") },
) : AccountRepository {
    var findAccountCallCount = 0
        private set
    var resetPasswordCallCount = 0
        private set
    var lastResetPasswordCall: ResetPasswordCall? = null
        private set

    override suspend fun sendFindCode(email: String): Result<Unit> = onSendFindCode()

    override suspend fun resetPassword(
        email: String,
        certificateCode: String,
        newPassword: String,
        confirmPassword: String,
    ): Result<Unit> {
        resetPasswordCallCount++
        lastResetPasswordCall = ResetPasswordCall(email, certificateCode, newPassword, confirmPassword)
        return onResetPassword()
    }

    /**
     * 호출을 세고 실패로 닫는다 — 던지면 호출 사실이 예외로 먼저 터져 «몇 번 불렸나» 를
     * 단언할 수 없다. 이 흐름에서 이 API 가 불리면 서버가 인증번호를 지워 최종 제출이 깨진다.
     */
    override suspend fun findAccount(
        email: String,
        certificateCode: String,
    ): Result<FoundAccount> {
        findAccountCallCount++
        return Result.failure(IllegalStateException("findAccount 는 비밀번호 찾기 흐름에서 호출되면 안 됨"))
    }

    override suspend fun sendEmailCode(email: String): Result<Unit> = error("sendEmailCode 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun verifyEmail(
        email: String,
        certificateCode: String,
    ): Result<Unit> = error("verifyEmail 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun signUp(
        email: String,
        password: String,
        name: String,
        profileUrl: String?,
    ): Result<AccountRegistration> = error("signUp 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun passwordChange(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit> = error("passwordChange 는 이 시나리오에서 호출되면 안 됨")
}
