package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.afternote.feature.onboarding.presentation.R as OnboardingR

/** API 34 Accessibility Test Framework로 첫 진입의 두 핵심 화면을 실제 렌더링해 검사한다. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class AccessibilitySmokeAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun enableChecks() {
        hiltRule.inject()
        composeRule.enableAccessibilityChecks()
    }

    @Test
    fun welcomeAndLogin_haveNoAutomatedAccessibilityErrors() {
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_welcome_start))
            .performClick()

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
