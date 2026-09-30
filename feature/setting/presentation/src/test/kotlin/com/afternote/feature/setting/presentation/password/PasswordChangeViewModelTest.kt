package com.afternote.feature.setting.presentation.password

import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R
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
import java.io.IOException

/**
 * 설정 > 비밀번호 변경의 제출 계약 회귀 가드 (#564).
 *
 * 지키는 것 셋 —
 * 1. 규칙을 만족하지 않는 입력은 **서버를 치기 전에** 막는다(서버 `@Pattern` 400 을 왕복해서 알지 않는다).
 * 2. 두 필드를 서버 계약 그대로 싣는다.
 * 3. 사유가 확인된 실패는 타입별 전용 문구로 갈리고, 확인되지 않은 실패만 폴백으로 내려앉는다.
 *
 * 저장소 가짜는 Java `Proxy` 가 아니라 실제 구현체다 — `Result` 를 돌려주는 suspend 함수는
 * Proxy 에서 이름이 맹글링돼 분기가 조용히 빗나간다(PR #1828).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordChangeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `새 비밀번호가 규칙을 어기면 제출이 저장소를 치지 않는다`() {
        val repository = FakeAccountRepository()
        val viewModel = PasswordChangeViewModel(repository)

        viewModel.onIntent(PasswordChangeIntent.UpdateCurrentPassword(CURRENT_PASSWORD))
        viewModel.onIntent(PasswordChangeIntent.UpdateNewPassword("weakpassword"))
        viewModel.onIntent(PasswordChangeIntent.Submit)

        assertFalse(viewModel.uiState.value.isNewPasswordRuleSatisfied)
        assertFalse(viewModel.uiState.value.isSubmitEnabled)
        assertEquals(emptyList<Pair<String, String>>(), repository.passwordChangeCalls)
    }

    @Test
    fun `현재 비밀번호가 비어 있으면 제출이 저장소를 치지 않는다`() {
        val repository = FakeAccountRepository()
        val viewModel = PasswordChangeViewModel(repository)

        viewModel.onIntent(PasswordChangeIntent.UpdateNewPassword(NEW_PASSWORD))
        viewModel.onIntent(PasswordChangeIntent.Submit)

        assertEquals(emptyList<Pair<String, String>>(), repository.passwordChangeCalls)
    }

    @Test
    fun `현재 비밀번호는 규칙 검사를 받지 않는다`() {
        val repository = FakeAccountRepository()
        val viewModel = PasswordChangeViewModel(repository)

        // 옛 규칙으로 만든 비밀번호 — 지금 규칙(특수문자 8종)에는 걸린다.
        viewModel.onIntent(PasswordChangeIntent.UpdateCurrentPassword("oldpassword"))
        viewModel.onIntent(PasswordChangeIntent.UpdateNewPassword(NEW_PASSWORD))
        viewModel.onIntent(PasswordChangeIntent.Submit)

        assertEquals(listOf("oldpassword" to NEW_PASSWORD), repository.passwordChangeCalls)
    }

    @Test
    fun `성공하면 두 필드를 그대로 싣고 완료 신호를 세운다`() {
        val repository = FakeAccountRepository()
        val viewModel = PasswordChangeViewModel(repository)

        submit(viewModel)

        assertEquals(listOf(CURRENT_PASSWORD to NEW_PASSWORD), repository.passwordChangeCalls)
        assertEquals(PasswordChanged, viewModel.uiState.value.changed)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `완료 신호는 소비하면 지워진다`() {
        val repository = FakeAccountRepository()
        val viewModel = PasswordChangeViewModel(repository)

        submit(viewModel)
        viewModel.onIntent(PasswordChangeIntent.ConsumeChanged)

        assertNull(viewModel.uiState.value.changed)
    }

    @Test
    fun `기존과 같은 비밀번호(1206)는 전용 문구로 갈린다`() {
        val repository = FakeAccountRepository(result = Result.failure(CoreAuthFailure.PasswordUnchanged(Exception("1206"))))
        val viewModel = PasswordChangeViewModel(repository)

        submit(viewModel)

        assertEquals(
            UiText.Resource(R.string.setting_password_change_unchanged),
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.changed)
    }

    @Test
    fun `소셜 가입 계정(1702)은 전용 문구로 갈린다`() {
        val repository = FakeAccountRepository(result = Result.failure(CoreAuthFailure.SocialSignUpAccount(Exception("1702"))))
        val viewModel = PasswordChangeViewModel(repository)

        submit(viewModel)

        assertEquals(
            UiText.Resource(R.string.setting_password_change_social_blocked),
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `전송 실패는 네트워크 문구로 갈린다`() {
        val repository = FakeAccountRepository(result = Result.failure(CoreAuthFailure.NetworkUnavailable(IOException("offline"))))
        val viewModel = PasswordChangeViewModel(repository)

        submit(viewModel)

        assertEquals(
            UiText.Resource(R.string.setting_password_change_network_error),
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `사유가 확인되지 않은 실패는 폴백 문구로 내려앉는다`() {
        val repository = FakeAccountRepository(result = Result.failure(IllegalStateException("1202")))
        val viewModel = PasswordChangeViewModel(repository)

        submit(viewModel)

        assertEquals(
            UiText.Resource(R.string.setting_password_change_failed),
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `입력을 고치면 직전 실패 안내가 지워진다`() {
        val repository = FakeAccountRepository(result = Result.failure(CoreAuthFailure.PasswordUnchanged(Exception("1206"))))
        val viewModel = PasswordChangeViewModel(repository)

        submit(viewModel)
        viewModel.onIntent(PasswordChangeIntent.UpdateNewPassword("NewPass2!"))

        assertNull(viewModel.uiState.value.errorMessage)
    }

    /**
     * 유니코드 숫자는 **허용 목록 밖 문자라서** 거절된다 — 서버와 같은 판정이다.
     *
     * `[0-9]` 를 `\d` 로 되돌리는 변이는 이 단언으로 잡히지 않는다(실측). 허용 목록
     * `[A-Za-z0-9@$!%*#?&]` 이 이미 그 문자들을 닫고 있어 두 정규식의 판정이 갈리는 입력 자체가
     * 없고, JVM 유닛 테스트는 호스트 정규식 엔진이라 Android 의 유니코드 문자 클래스도 재현하지
     * 못한다. `\d` 를 쓰지 않는 이유는 허용 목록이 넓어지는 날 갈리기 때문이고, 그 근거는
     * [SettingPasswordRule] KDoc 이 갖는다.
     */
    @Test
    fun `규칙은 서버와 같다 - 유니코드 숫자를 숫자로 받지 않는다`() {
        assertFalse(SettingPasswordRule.isSatisfied("Abcdefg１!"))
        assertFalse(SettingPasswordRule.isSatisfied("Abcdefg١!"))
        assertTrue(SettingPasswordRule.isSatisfied("Abcdefg1!"))
    }

    @Test
    fun `규칙은 서버와 같다 - 허용 목록 밖 문자와 길이를 막는다`() {
        assertFalse(SettingPasswordRule.isSatisfied("Abcde1!"))
        assertFalse(SettingPasswordRule.isSatisfied("Abcdefghijklmn1!"))
        assertFalse(SettingPasswordRule.isSatisfied("Abcdefg1_"))
        assertFalse(SettingPasswordRule.isSatisfied("Abcdefg1 !"))
        assertFalse(SettingPasswordRule.isSatisfied("Abcdefgh!"))
        assertFalse(SettingPasswordRule.isSatisfied("Abcdefg12"))
        assertTrue(SettingPasswordRule.isSatisfied("Abcdefghijklm1!"))
    }

    private fun submit(viewModel: PasswordChangeViewModel) {
        viewModel.onIntent(PasswordChangeIntent.UpdateCurrentPassword(CURRENT_PASSWORD))
        viewModel.onIntent(PasswordChangeIntent.UpdateNewPassword(NEW_PASSWORD))
        viewModel.onIntent(PasswordChangeIntent.Submit)
    }

    private companion object {
        const val CURRENT_PASSWORD = "OldPass1!"
        const val NEW_PASSWORD = "NewPass1!"
    }
}

private class FakeAccountRepository(
    private val result: Result<Unit> = Result.success(Unit),
) : AccountRepository {
    val passwordChangeCalls = mutableListOf<Pair<String, String>>()

    override suspend fun passwordChange(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit> {
        passwordChangeCalls += currentPassword to newPassword
        return result
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

    override suspend fun signUp(
        email: String,
        password: String,
        name: String,
        profileUrl: String?,
    ): Result<AccountRegistration> = error("signUp 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun resetPassword(
        email: String,
        certificateCode: String,
        newPassword: String,
        confirmPassword: String,
    ): Result<Unit> = error("resetPassword 는 이 시나리오에서 호출되면 안 됨")
}
