package com.afternote.feature.onboarding.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.signup.SignUpScreen
import com.afternote.feature.onboarding.presentation.terms.OnboardingTermsScreen
import com.afternote.feature.onboarding.presentation.terms.TermsState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class OnboardingContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun signUpRequiredInputs_gateNextAndKeepEnteredValues() {
        var email by mutableStateOf("")
        var code by mutableStateOf("")
        var nextCalls = 0
        composeRule.setContent {
            AfternoteTheme {
                SignUpScreen(
                    initialEmail = email,
                    initialVerificationCode = code,
                    isVerificationSent = true,
                    isSendingCode = false,
                    isEmailFormatValid = email.contains('@'),
                    resendCooldownSeconds = 0,
                    hasVerificationError = false,
                    isNextEnabled = email.contains('@') && code.length == 6,
                    snackbarHostState = remember { SnackbarHostState() },
                    onEmailChange = { email = it },
                    onVerificationCodeChange = { code = it },
                    onRequestVerification = {},
                    onNextClick = { nextCalls += 1 },
                    onBackClick = {},
                )
            }
        }

        composeRule.onNode(hasText("다음") and hasClickAction()).assertIsNotEnabled()
        composeRule.onNodeWithText("이메일 주소").performTextInput("new@example.test")
        composeRule.onNodeWithText("인증번호").performTextInput("123456")
        composeRule.onNode(hasText("다음") and hasClickAction()).assertIsEnabled().performClick()

        assertEquals(1, nextCalls)
        assertEquals("new@example.test", email)
        assertEquals("123456", code)
    }

    @Test
    fun requiredTerms_enableNext_withoutOptionalMarketing() {
        var state by mutableStateOf(TermsState())
        var nextCalls = 0
        composeRule.setContent {
            AfternoteTheme {
                OnboardingTermsScreen(
                    termsState = state,
                    isNextEnabled = state.isTermsAgreed && state.isPrivacyAgreed,
                    snackbarHostState = remember { SnackbarHostState() },
                    onTermsToggle = { state = state.copy(isTermsAgreed = it) },
                    onPrivacyToggle = { state = state.copy(isPrivacyAgreed = it) },
                    onMarketingToggle = { state = state.copy(isMarketingAgreed = it) },
                    onToggleAll = {
                        state = TermsState(it, it, it)
                    },
                    onViewTermsClick = {},
                    onNextClick = { nextCalls += 1 },
                    onBackClick = {},
                )
            }
        }

        composeRule.onNode(hasText("다음") and hasClickAction()).assertIsNotEnabled()
        composeRule.onNodeWithText("애프터노트 서비스 이용 약관 (필수)").performClick()
        composeRule.onNode(hasText("다음") and hasClickAction()).assertIsNotEnabled()
        composeRule.onNodeWithText("개인정보 수집 및 이용 동의서 (필수)").performClick()
        composeRule
            .onNode(hasText("다음") and hasClickAction())
            .assertIsEnabled()
            .performClick()

        assertEquals(false, state.isMarketingAgreed)
        assertEquals(1, nextCalls)
    }

    @Test
    fun signUpTextFieldState_survivesSavedInstanceStateRestore() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            AfternoteTheme {
                SignUpScreen(
                    initialEmail = "",
                    initialVerificationCode = "",
                    isVerificationSent = true,
                    isSendingCode = false,
                    isEmailFormatValid = true,
                    resendCooldownSeconds = 0,
                    hasVerificationError = false,
                    isNextEnabled = false,
                    snackbarHostState = remember { SnackbarHostState() },
                    onEmailChange = {},
                    onVerificationCodeChange = {},
                    onRequestVerification = {},
                    onNextClick = {},
                    onBackClick = {},
                )
            }
        }
        composeRule.onNodeWithText("이메일 주소").performTextInput("restore@example.test")
        composeRule.onNodeWithText("인증번호").performTextInput("654321")

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("restore@example.test").assertExists()
        composeRule.onNodeWithText("654321").assertExists()
    }
}
