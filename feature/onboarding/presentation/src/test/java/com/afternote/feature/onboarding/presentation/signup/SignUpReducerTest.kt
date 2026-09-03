package com.afternote.feature.onboarding.presentation.signup

import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.feature.onboarding.presentation.NoopErrorReporter
import com.afternote.feature.onboarding.presentation.UnusedAccountRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 회원가입의 **순수 전이** (#1802).
 *
 * 코루틴 하네스가 없다 — 여기서 보는 Intent 는 저장소를 부르지 않고 `dispatch` → `reduce` 만
 * 지난다. 제출 잠금·부분 성공 복구 같은 비동기 갈래는 [SignUpViewModelSubmitTest] 가 본다.
 *
 * Robolectric 은 [SignUpUiState.isEmailFormatValid] 가 읽는 `android.util.Patterns` 때문에만
 * 필요하다 — 순수 JVM 에서는 그 상수가 null 이다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SignUpReducerTest {
    private fun viewModel(): SignUpViewModel =
        SignUpViewModel(
            accountRepository = UnusedAccountRepository,
            loginUseCase = LoginUseCase(FakeAuthRepository.strict()),
            errorReporter = NoopErrorReporter,
        )

    @Test
    fun `이메일과 인증번호가 바뀌면 인증 에러가 지워진다`() {
        val viewModel = viewModel()

        viewModel.onIntent(SignUpIntent.UpdateEmail("user@example.com"))
        viewModel.onIntent(SignUpIntent.UpdateVerificationCode("000000"))

        val state = viewModel.uiState.value
        assertEquals("user@example.com", state.email)
        assertEquals("000000", state.verificationCode)
        assertFalse(state.hasVerificationError)
    }

    @Test
    fun `전체 동의는 세 항목을 한 번에 뒤집는다`() {
        val viewModel = viewModel()

        viewModel.onIntent(SignUpIntent.ToggleAllTerms(true))

        val agreed = viewModel.uiState.value.termsState
        assertTrue(agreed.isTermsAgreed)
        assertTrue(agreed.isPrivacyAgreed)
        assertTrue(agreed.isMarketingAgreed)
        assertTrue(viewModel.uiState.value.isStep4NextEnabled)

        viewModel.onIntent(SignUpIntent.ToggleMarketingAgreed(false))

        // 선택 항목만 내려가도 필수 두 개가 남아 "다음" 은 열려 있다.
        assertFalse(viewModel.uiState.value.termsState.isMarketingAgreed)
        assertTrue(viewModel.uiState.value.isStep4NextEnabled)

        viewModel.onIntent(SignUpIntent.TogglePrivacyAgreed(false))

        assertFalse(viewModel.uiState.value.isStep4NextEnabled)
    }

    @Test
    fun `단계별 다음 활성은 입력에서 파생된다`() {
        val viewModel = viewModel()

        viewModel.onIntent(SignUpIntent.UpdateEmail("user@example.com"))
        viewModel.onIntent(SignUpIntent.UpdateVerificationCode("12345"))
        assertFalse(viewModel.uiState.value.isStep1NextEnabled)

        viewModel.onIntent(SignUpIntent.UpdateVerificationCode("123456"))
        assertTrue(viewModel.uiState.value.isStep1NextEnabled)

        viewModel.onIntent(SignUpIntent.UpdateResidentFrontNumber("900101"))
        viewModel.onIntent(SignUpIntent.UpdateResidentBackNumber("1"))
        assertTrue(viewModel.uiState.value.isStep2NextEnabled)

        viewModel.onIntent(SignUpIntent.UpdateSignUpPassword("Password1!"))
        assertFalse(viewModel.uiState.value.isStep3NextEnabled)

        viewModel.onIntent(SignUpIntent.UpdateSignUpPasswordConfirm("Password1!"))
        assertTrue(viewModel.uiState.value.isStep3NextEnabled)
    }

    @Test
    fun `이름이 비면 제출이 저장소로 가지 않고 안내 신호만 선다`() {
        val viewModel = viewModel()
        viewModel.onIntent(SignUpIntent.UpdateName("   "))

        viewModel.onIntent(SignUpIntent.SubmitSignUp)

        assertTrue(viewModel.uiState.value.isNameRequired)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onIntent(SignUpIntent.ConsumeNameRequired)

        assertFalse(viewModel.uiState.value.isNameRequired)
    }

    @Test
    fun `소비 Intent 는 그 신호만 되돌린다`() {
        val viewModel = viewModel()
        viewModel.onIntent(SignUpIntent.UpdateName("애프터노트"))

        viewModel.onIntent(SignUpIntent.ConsumeSignedUp)
        viewModel.onIntent(SignUpIntent.ConsumeResidentNumberNavigation)
        viewModel.onIntent(SignUpIntent.ConsumeError)

        val state = viewModel.uiState.value
        assertFalse(state.isSignedUp)
        assertFalse(state.shouldNavigateToResidentNumber)
        assertNull(state.errorMessage)
        assertEquals("애프터노트", state.name)
    }

    @Test
    fun `프로필 이미지 선택은 문자열로 보관된다`() {
        val viewModel = viewModel()

        viewModel.onIntent(SignUpIntent.PickProfileImage("content://media/picked"))

        assertEquals("content://media/picked", viewModel.uiState.value.profileImageUri)
    }
}
