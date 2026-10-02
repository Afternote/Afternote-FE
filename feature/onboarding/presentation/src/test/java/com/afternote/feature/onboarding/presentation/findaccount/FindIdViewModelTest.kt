package com.afternote.feature.onboarding.presentation.findaccount

import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.feature.onboarding.presentation.NoopErrorReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 아이디 찾기의 **연타 가드** (#1864).
 *
 * [FindIdReducerTest] 가 보는 순수 전이와 갈라 둔다 — 여기서 보는 것은 상태가 아니라
 * 「서버 호출이 몇 번 나갔는가」 다.
 *
 * 두 함수 다 상태 플래그(`isSendingCode`·`isVerifying`)만으로는 못 막는다. 가드가
 * `viewModelScope.launch` **밖**에서 상태를 읽는데 launch 본문은 다음 루프에 돌므로, 플래그가
 * 상태에 반영되기 전에 두 번째 호출이 같은 `false` 를 읽고 통과한다.
 *
 * **[StandardTestDispatcher] 여야 그 창이 재현된다.** `UnconfinedTestDispatcher` 는 launch 본문을
 * 즉시 돌려 플래그가 이미 반영돼 있으므로, 결함이 있는 코드에서도 이 테스트가 통과한다 —
 * 비밀번호 찾기에서 실제로 그렇게 통과하고 있었다 (#457).
 *
 * Robolectric 은 [FindIdUiState.isEmailFormatValid] 가 읽는 `android.util.Patterns` 때문에만 필요하다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FindIdViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 중복 발송은 코드를 새로 발급해 앞서 안내한 인증번호를 조용히 무효로 만든다. */
    @Test
    fun `인증번호 발송 연타는 상태가 반영되기 전에도 요청을 한 번만 낸다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = CountingAccountRepository()
            val viewModel = viewModel(repository)
            viewModel.onIntent(FindIdIntent.UpdateEmail(EMAIL))

            viewModel.onIntent(FindIdIntent.RequestVerificationCode)
            viewModel.onIntent(FindIdIntent.RequestVerificationCode)
            advanceUntilIdle()

            assertEquals(1, repository.sendFindCodeCalls)
        }

    /**
     * `auth/email/find` 는 인증번호를 검증하며 **지운다**(BE `EmailService.verifyAndDeleteCode`).
     * 두 번째 요청은 코드가 이미 없어 1207 로 실패하고, 첫 요청이 성공했는데도 사용자에게는
     * 「인증번호가 일치하지 않습니다」 가 뜬다.
     */
    @Test
    fun `인증번호 확인 연타는 상태가 반영되기 전에도 조회를 한 번만 부른다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = CountingAccountRepository()
            val viewModel = readyToVerify(repository)

            viewModel.onIntent(FindIdIntent.VerifyCode)
            viewModel.onIntent(FindIdIntent.VerifyCode)
            advanceUntilIdle()

            assertEquals(1, repository.findAccountCalls)
        }

    /** 앞 요청이 끝난 뒤의 재확인까지 막지는 않는다 — 가드는 「진행 중」만 본다. */
    @Test
    fun `앞 확인이 끝난 뒤의 재확인은 다시 조회한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = CountingAccountRepository()
            val viewModel = readyToVerify(repository)

            viewModel.onIntent(FindIdIntent.VerifyCode)
            advanceUntilIdle()
            viewModel.onIntent(FindIdIntent.VerifyCode)
            advanceUntilIdle()

            assertEquals(2, repository.findAccountCalls)
        }

    private fun viewModel(repository: AccountRepository) = FindIdViewModel(repository, NoopErrorReporter)

    /** 발송을 끝내 `isVerifyEnabled` 를 연 상태 — 확인 연타가 통과할 수 있는 자리다. */
    private suspend fun TestScope.readyToVerify(repository: CountingAccountRepository): FindIdViewModel {
        val viewModel = viewModel(repository)
        viewModel.onIntent(FindIdIntent.UpdateEmail(EMAIL))
        viewModel.onIntent(FindIdIntent.RequestVerificationCode)
        advanceUntilIdle()
        viewModel.onIntent(FindIdIntent.UpdateCertificateCode(CODE))
        return viewModel
    }

    private class CountingAccountRepository : AccountRepository {
        var sendFindCodeCalls = 0
        var findAccountCalls = 0

        override suspend fun sendFindCode(email: String): Result<Unit> {
            sendFindCodeCalls++
            return Result.success(Unit)
        }

        override suspend fun findAccount(
            email: String,
            certificateCode: String,
        ): Result<FoundAccount> {
            findAccountCalls++
            return Result.success(FoundAccount(name = "박서연", email = email))
        }

        override suspend fun sendEmailCode(email: String): Result<Unit> = error("이 테스트가 부르지 않는 경로")

        override suspend fun verifyEmail(
            email: String,
            certificateCode: String,
        ): Result<Unit> = error("이 테스트가 부르지 않는 경로")

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
        const val EMAIL = "user@example.com"
        const val CODE = "123456"
    }
}
