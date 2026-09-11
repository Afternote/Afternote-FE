package com.afternote.afternote_fe

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.navigation.rememberAfternoteAppState
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.HiltTestActivity
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingImplementedCoverageAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Inject
    lateinit var userRepository: UserRepository

    private val fakeUserRepository get() = userRepository as FakeUserRepository

    private lateinit var navController: NavHostController
    private lateinit var restorationTester: StateRestorationTester

    @Before
    fun setUp() {
        hiltRule.inject()
        fakeUserRepository.onGetReceiverDetail = null
        fakeUserRepository.onGetReceiverDeliveryConditions = null
        restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            val appState = rememberAfternoteAppState()
            SideEffect { navController = appState.navController }
            AfternoteTheme {
                AppNavigation(
                    startDestination = Route.Setting(),
                    appState = appState,
                )
            }
        }
    }

    @Test
    fun actualSettingNavHost_receiverManageListRowNavigatesToEditWithExactReceiverId() {
        waitForRootHost()
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("수신자 목록")).run {
            assertCountEquals(2)
            get(1).performScrollTo().performClick()
        }
        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onAllNodes(checkboxMatcher).assertCountEquals(0)

        composeRule.onNodeWithText("김수신").performClick()

        waitForText("수신자 수정")
        assertEquals(RECEIVER_ID, fakeUserRepository.receiverDetailCalls.last())
    }

    @Test
    fun actualSettingNavHost_deliveryConditionReceiverSelectionPreservesExactReceiverId() {
        waitForRootHost()
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

        waitForText("마지막 인사말 수정하기")
        assertEquals(RECEIVER_ID, fakeUserRepository.deliveryLoadCalls.last())
        composeRule
            .onNodeWithText("마지막 인사말 수정하기")
            .assertIsDisplayed()
            .performClick()

        waitForText("수신자 수정")
        assertEquals(RECEIVER_ID, fakeUserRepository.receiverDetailCalls.last())
    }

    @Test
    fun actualSettingNavHost_withdrawGuideCancelThenAgreementConfirmPreservesBoundary() {
        waitForRootHost()
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

        waitForRootHost()
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

        waitForText("안전한 탈퇴 진행을 위해 아래 문장을 입력해 주세요.")
        composeRule
            .onNodeWithText("안전한 탈퇴 진행을 위해 아래 문장을 입력해 주세요.")
            .assertIsDisplayed()
    }

    @Test
    fun actualSettingNavHost_savedRouteAndEditInputRestoreThenBackReturnsToReceiverList() {
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("수신자 목록"))[1].performScrollTo().performClick()
        composeRule.onNodeWithText("김수신").performClick()
        waitForText("수신자 수정")
        composeRule.onNodeWithText("김수신").performTextInput("복원")

        restorationTester.emulateSavedInstanceStateRestore()

        waitForText("김수신복원")
        assertEquals(RECEIVER_ID, fakeUserRepository.receiverDetailCalls.last())
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText("김수신")
        composeRule.onAllNodes(checkboxMatcher).assertCountEquals(0)
        waitForRootHost()
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

        waitForText("비밀번호를 잊어버렸어요.")
        composeRule
            .onNodeWithText("비밀번호를 잊어버렸어요.")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .performClick()
        waitForSettingHomeContent()
    }

    @Test
    fun actualSettingNavHost_customerCenterProfileShortcutAndMenuBothNavigateAndBack() {
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("고객센터")).run {
            assertCountEquals(2)
            get(0).performClick()
        }
        waitForText("전화 문의")
        composeRule.onNodeWithText("고객센터").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .performClick()

        waitForSettingHomeContent()
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("고객센터")).run {
            assertCountEquals(2)
            get(1).performScrollTo().performClick()
        }
        waitForText("전화 문의")
        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .performClick()
        waitForSettingHomeContent()
    }

    @Test
    fun actualCustomerCenterScreen_phoneClickFiresDialIntentAndEmailClickCopiesAddressWithSnackbar() {
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("고객센터")).run {
            assertCountEquals(2)
            get(1).performScrollTo().performClick()
        }
        waitForText("전화 문의")

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
        waitForText("전화 문의")

        composeRule.onNodeWithText("유족·수신자 전용 문의").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun actualCustomerCenterScreen_inquiryAndFaqMenusNavigateAndReturnToHub() {
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("고객센터"))[0].performClick()
        waitForText("전화 문의")

        composeRule.onNodeWithText("1:1 문의").performClick()
        waitForText("새 문의 접수하기")
        composeRule.onNodeWithText("새 문의 접수하기").performClick()
        waitForText("제목을 입력해 주세요.")
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        waitForText("새 문의 접수하기")
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        waitForText("전화 문의")

        composeRule.onNodeWithText("자주 묻는 질문").performScrollTo().performClick()
        waitForText("비밀번호를 잊어버렸어요.")
        composeRule.onNodeWithText("비밀번호를 잊어버렸어요.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        waitForText("전화 문의")
    }

    private fun openWithdrawGuide() {
        waitForSettingHomeContent()
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("회원 탈퇴"))
        composeRule
            .onNodeWithText("회원 탈퇴")
            .performClick()
        waitForText("회원 탈퇴 안내")
    }

    private fun waitForSettingHomeContent() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText("프로필 수정")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForRootHost() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            navController.currentDestination?.hasRoute<Route.Setting>() == true
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val RECEIVER_ID = 7L
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}
