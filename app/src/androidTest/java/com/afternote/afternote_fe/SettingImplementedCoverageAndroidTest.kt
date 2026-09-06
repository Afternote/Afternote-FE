package com.afternote.afternote_fe

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.navigation.AppState
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingImplementedCoverageAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activityRule.scenario.onActivity { activity ->
            navController =
                TestNavHostController(activity).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }
            activity.setContent {
                AfternoteTheme {
                    AppNavigation(
                        startDestination = Route.Setting,
                        appState = AppState(navController),
                    )
                }
            }
        }
    }

    @Test
    fun actualSettingNavHost_receiverSelectionPreservesNormalBackAndExactDeliveryReceiverId() {
        waitForRoute<SettingRoute.SettingHomeRoute>()
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("수신자 목록")).run {
            assertCountEquals(2)
            get(1).performScrollTo().performClick()
        }
        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onAllNodes(checkboxMatcher).run {
            assertCountEquals(1)
            get(0).performClick()
        }
        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()

        waitForRoute<SettingRoute.SettingHomeRoute>()
        waitForSettingHomeContent()
        composeRule
            .onNodeWithText("사후 전달 조건")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onAllNodes(checkboxMatcher).run {
            assertCountEquals(1)
            get(0).performClick()
        }
        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()

        val deliveryRoute = waitForRoute<SettingRoute.AfterDeliveryRoute>()
        assertEquals(RECEIVER_ID, deliveryRoute.receiverId)
        composeRule
            .onNodeWithText("마지막 인사말 수정하기")
            .assertIsDisplayed()
            .performClick()

        val editRoute = waitForRoute<SettingRoute.RecipientEditRoute>()
        assertEquals(RECEIVER_ID, editRoute.receiverId)
    }

    @Test
    fun actualSettingNavHost_withdrawGuideCancelThenAgreementConfirmPreservesBoundary() {
        waitForRoute<SettingRoute.SettingHomeRoute>()
        openWithdrawGuide()
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("탈퇴하기"))
        composeRule
            .onNodeWithText("탈퇴하기")
            .assertIsNotEnabled()
        composeRule
            .onNodeWithText("취소하기")
            .performClick()

        waitForRoute<SettingRoute.SettingHomeRoute>()
        openWithdrawGuide()
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("탈퇴하기"))
        composeRule.onAllNodes(checkboxMatcher).run {
            assertCountEquals(1)
            get(0).performClick()
        }
        composeRule
            .onNodeWithText("탈퇴하기")
            .performClick()

        waitForRoute<SettingRoute.WithdrawConfirmRoute>()
        composeRule
            .onNodeWithText("안전한 탈퇴 진행을 위해 아래 문장을 입력해 주세요.")
            .assertIsDisplayed()
    }

    @Test
    fun actualSettingNavHost_customerCenterProfileShortcutAndMenuBothNavigateAndBack() {
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("고객센터")).run {
            assertCountEquals(2)
            get(0).performClick()
        }
        waitForRoute<SettingRoute.CustomerCenterRoute>()
        composeRule.onNodeWithText("고객센터").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .performClick()

        waitForRoute<SettingRoute.SettingHomeRoute>()
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("고객센터")).run {
            assertCountEquals(2)
            get(1).performScrollTo().performClick()
        }
        waitForRoute<SettingRoute.CustomerCenterRoute>()
        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .performClick()
        waitForRoute<SettingRoute.SettingHomeRoute>()
    }

    @Test
    fun actualCustomerCenterScreen_phoneClickFiresDialIntentAndEmailClickCopiesAddressWithSnackbar() {
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("고객센터")).run {
            assertCountEquals(2)
            get(1).performScrollTo().performClick()
        }
        waitForRoute<SettingRoute.CustomerCenterRoute>()

        Intents.init()
        try {
            Intents
                .intending(hasAction(Intent.ACTION_DIAL))
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
            composeRule.onNodeWithText("전화 문의").performClick()
            Intents.intended(
                allOf(
                    hasAction(Intent.ACTION_DIAL),
                    hasData(Uri.parse("tel:15880000")),
                ),
            )
        } finally {
            Intents.release()
        }

        composeRule.onNodeWithText("이메일 문의").performClick()
        composeRule.onNodeWithText("이메일 주소가 복사되었습니다.").assertIsDisplayed()

        val clipboardManager =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getSystemService(ClipboardManager::class.java)
        assertEquals(
            "help@afternote.app",
            clipboardManager.primaryClip
                ?.getItemAt(0)
                ?.text
                .toString(),
        )
    }

    @Test
    fun actualCustomerCenterScreen_recipientInquiryIsDisabled() {
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("고객센터")).run {
            assertCountEquals(2)
            get(1).performScrollTo().performClick()
        }
        waitForRoute<SettingRoute.CustomerCenterRoute>()

        composeRule.onNodeWithText("유족·수신자 전용 문의").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun actualSettingNavHost_faqRowNavigatesToFaqScreenAndBackReturnsHome() {
        waitForSettingHomeContent()
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("FAQ"))
        composeRule
            .onNodeWithText("FAQ")
            .performClick()

        waitForRoute<SettingRoute.FaqRoute>()
        composeRule
            .onNodeWithText("비밀번호를 잊어버렸어요.")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .performClick()
        waitForRoute<SettingRoute.SettingHomeRoute>()
    }

    @Test
    fun actualCustomerCenterScreen_inquiryAndFaqMenusNavigateAndReturnToHub() {
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("고객센터"))[0].performClick()
        waitForRoute<SettingRoute.CustomerCenterRoute>()

        composeRule.onNodeWithText("1:1 문의").performClick()
        waitForRoute<SettingRoute.InquiryListRoute>()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        waitForRoute<SettingRoute.CustomerCenterRoute>()

        composeRule.onNodeWithText("자주 묻는 질문").performScrollTo().performClick()
        waitForRoute<SettingRoute.FaqRoute>()
        composeRule.onNodeWithText("비밀번호를 잊어버렸어요.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        waitForRoute<SettingRoute.CustomerCenterRoute>()
    }

    private fun openWithdrawGuide() {
        waitForSettingHomeContent()
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("회원 탈퇴"))
        composeRule
            .onNodeWithText("회원 탈퇴")
            .performClick()
        waitForRoute<SettingRoute.WithdrawGuideRoute>()
    }

    private fun waitForSettingHomeContent() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText("프로필 수정")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private inline fun <reified T : Any> waitForRoute(): T {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            navController.currentDestination?.hasRoute<T>() == true
        }
        return composeRule.runOnIdle {
            navController.currentBackStackEntry?.toRoute<T>()
                ?: error("Current back stack entry is missing")
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val RECEIVER_ID = 7L
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}
