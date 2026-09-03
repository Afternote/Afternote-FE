package com.afternote.feature.onboarding.presentation.findaccount

import com.afternote.core.model.FoundAccount
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
 * 아이디 찾기의 **순수 전이** (#1802).
 *
 * 코루틴 하네스가 없다 — `Dispatchers.setMain` 도, `runTest` 도 쓰지 않는다. 여기서 보는
 * Intent 는 저장소를 부르지 않고 `dispatch` → `reduce` 만 지나기 때문이고, 그것이 MVI 로
 * 옮긴 이득이다. 비동기·부수효과는 ViewModel 테스트가 본다.
 *
 * [UnusedAccountRepository] 는 어느 경로가 불려도 `error` 로 드러내므로, 이 파일이 초록인 것
 * 자체가 「이 Intent 들은 저장소를 부르지 않는다」 의 증거다.
 *
 * Robolectric 은 [FindIdUiState.isEmailFormatValid] 가 읽는 `android.util.Patterns` 때문에만
 * 필요하다 — 순수 JVM 에서는 그 상수가 null 이다. 시간·디스패처는 여전히 개입하지 않는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FindIdReducerTest {
    private fun viewModel() = FindIdViewModel(UnusedAccountRepository, NoopErrorReporter)

    @Test
    fun `이메일이 바뀌면 앞서 받은 인증 에러가 함께 지워진다`() {
        val viewModel = viewModel()

        viewModel.onIntent(FindIdIntent.UpdateEmail("user@example.com"))
        viewModel.onIntent(FindIdIntent.UpdateCertificateCode("123456"))

        assertEquals("user@example.com", viewModel.uiState.value.email)
        assertEquals("123456", viewModel.uiState.value.certificateCode)

        viewModel.onIntent(FindIdIntent.UpdateEmail("other@example.com"))

        val state = viewModel.uiState.value
        assertNull(state.foundAccount)
        assertFalse(state.hasVerificationError)
    }

    @Test
    fun `ConsumeError 는 스낵바 신호만 지우고 입력은 남긴다`() {
        val viewModel = viewModel()
        viewModel.onIntent(FindIdIntent.UpdateEmail("user@example.com"))

        viewModel.onIntent(FindIdIntent.ConsumeError)

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("user@example.com", state.email)
    }

    @Test
    fun `이메일 형식이 아니면 인증번호 요청이 저장소로 가지 않는다`() {
        val viewModel = viewModel()
        viewModel.onIntent(FindIdIntent.UpdateEmail("not-an-email"))

        viewModel.onIntent(FindIdIntent.RequestVerificationCode)

        assertFalse(viewModel.uiState.value.isSendingCode)
    }

    @Test
    fun `확인이 열리지 않은 상태에서는 검증이 저장소로 가지 않는다`() {
        val viewModel = viewModel()
        viewModel.onIntent(FindIdIntent.UpdateEmail("user@example.com"))
        viewModel.onIntent(FindIdIntent.UpdateCertificateCode("123"))

        viewModel.onIntent(FindIdIntent.VerifyCode)

        assertFalse(viewModel.uiState.value.isVerifying)
    }

    @Test
    fun `버튼 활성은 상태에서 파생된다`() {
        val sent =
            FindIdUiState(
                email = "user@example.com",
                certificateCode = "123456",
                isVerificationSent = true,
            )

        assertTrue(sent.isSendCodeEnabled)
        assertTrue(sent.isVerifyEnabled)
        // "다음" 은 확인으로 계정을 받아야만 열린다.
        assertFalse(sent.isNextEnabled)
        assertTrue(sent.copy(foundAccount = FoundAccount(name = "사용자", email = "user@example.com")).isNextEnabled)
        assertFalse(sent.copy(resendCooldownSeconds = 20).isSendCodeEnabled)
    }
}
