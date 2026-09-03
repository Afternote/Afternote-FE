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
import com.afternote.feature.onboarding.presentation.signup.SignUpContent
import com.afternote.feature.onboarding.presentation.signup.SignUpIntent
import com.afternote.feature.onboarding.presentation.signup.SignUpUiState
import com.afternote.feature.onboarding.presentation.terms.OnboardingTermsContent
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
        // 「다음」 활성은 이제 UiState 파생값이다 — 화면이 받는 것은 상태와 Intent 둘뿐이라,
        // VM 이 하는 전이를 테스트가 대신한다.
        var state by mutableStateOf(SignUpUiState(isVerificationSent = true))
        var nextCalls = 0
        composeRule.setContent {
            AfternoteTheme {
                SignUpContent(
                    state = state,
                    onIntent = { intent ->
                        when (intent) {
                            is SignUpIntent.UpdateEmail -> state = state.copy(email = intent.value)
                            is SignUpIntent.UpdateVerificationCode -> state = state.copy(verificationCode = intent.value)
                            SignUpIntent.VerifyEmailAndProceed -> nextCalls += 1
                            else -> Unit
                        }
                    },
                    snackbarHostState = remember { SnackbarHostState() },
                    onBackClick = {},
                )
            }
        }

        composeRule.onNode(hasText("다음") and hasClickAction()).assertIsNotEnabled()
        composeRule.onNodeWithText("이메일 주소").performTextInput("new@example.test")
        composeRule.onNodeWithText("인증번호").performTextInput("123456")
        composeRule.onNode(hasText("다음") and hasClickAction()).assertIsEnabled().performClick()

        assertEquals(1, nextCalls)
        assertEquals("new@example.test", state.email)
        assertEquals("123456", state.verificationCode)
    }

    @Test
    fun requiredTerms_enableNext_withoutOptionalMarketing() {
        var state by mutableStateOf(SignUpUiState())
        var nextCalls = 0
        composeRule.setContent {
            AfternoteTheme {
                OnboardingTermsContent(
                    state = state,
                    onIntent = { intent ->
                        val terms = state.termsState
                        state =
                            when (intent) {
                                is SignUpIntent.ToggleTermsAgreed -> {
                                    state.copy(termsState = terms.copy(isTermsAgreed = intent.agreed))
                                }

                                is SignUpIntent.TogglePrivacyAgreed -> {
                                    state.copy(termsState = terms.copy(isPrivacyAgreed = intent.agreed))
                                }

                                is SignUpIntent.ToggleMarketingAgreed -> {
                                    state.copy(termsState = terms.copy(isMarketingAgreed = intent.agreed))
                                }

                                is SignUpIntent.ToggleAllTerms -> {
                                    state.copy(
                                        termsState =
                                            terms.copy(
                                                isTermsAgreed = intent.agreed,
                                                isPrivacyAgreed = intent.agreed,
                                                isMarketingAgreed = intent.agreed,
                                            ),
                                    )
                                }

                                else -> {
                                    state
                                }
                            }
                    },
                    snackbarHostState = remember { SnackbarHostState() },
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

        assertEquals(false, state.termsState.isMarketingAgreed)
        assertEquals(1, nextCalls)
    }

    @Test
    fun signUpTextFieldState_survivesSavedInstanceStateRestore() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            AfternoteTheme {
                SignUpContent(
                    state = SignUpUiState(isVerificationSent = true),
                    onIntent = {},
                    snackbarHostState = remember { SnackbarHostState() },
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
