package com.afternote.afternote_fe

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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.navigation.AppState
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.data.local.ReceiverAuthCodeDataSource
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingImplementedCoverageAndroidTest {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var senderRegistry: SenderRegistry

    @Inject
    lateinit var receiverAuthCodeDataSource: ReceiverAuthCodeDataSource

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

    private val fakeAuth get() = authRepository as FakeAuthRepository

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

    @After
    fun tearDown() {
        runBlocking { receiverAuthCodeDataSource.saveCode("") }
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
    fun actualSettingNavHost_receivedRecordsEntryPreservesMemberAndReceiverContextsOnBack() {
        fakeAuth.loggedIn = true
        fakeAuth.accessToken = MEMBER_ACCESS_TOKEN
        fakeAuth.refreshToken = MEMBER_REFRESH_TOKEN
        val senderId = seedReceiverContext()

        waitForRoute<SettingRoute.SettingHomeRoute>()
        waitForSettingHomeContent()
        assertReceiverContextUntouched(senderId)
        composeRule
            .onNodeWithText("받은 기록 확인하기")
            .performScrollTo()
            .performClick()

        waitForRoute<ReceiverRoute.ReceivedRecordsRoute>()
        composeRule.onNodeWithText(SENDER_ALIAS).assertIsDisplayed()
        assertMemberSessionUntouched()
        assertReceiverContextUntouched(senderId)

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        waitForRoute<SettingRoute.SettingHomeRoute>()
        assertMemberSessionUntouched()
        assertReceiverContextUntouched(senderId)
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

    private fun assertMemberSessionUntouched() {
        assertTrue(fakeAuth.loggedIn)
        assertEquals(MEMBER_ACCESS_TOKEN, fakeAuth.accessToken)
        assertEquals(MEMBER_REFRESH_TOKEN, fakeAuth.refreshToken)
        assertEquals(0, fakeAuth.logoutCalls)
        assertEquals(0, fakeAuth.clearSessionCalls)
    }

    private fun seedReceiverContext(): String {
        val sender = senderRegistry.register(SENDER_ALIAS)
        val identity =
            ReceiverIdentity(
                receiverId = RECEIVER_ID,
                receiverName = RECEIVER_NAME,
                senderName = SENDER_NAME,
                relation = SENDER_RELATION,
            )
        checkNotNull(senderRegistry.attachIdentity(sender.id, RECEIVER_AUTH_CODE, identity))
        runBlocking { receiverAuthCodeDataSource.saveCode(RECEIVER_AUTH_CODE) }
        return sender.id
    }

    private fun assertReceiverContextUntouched(senderId: String) {
        val sender = checkNotNull(senderRegistry.findById(senderId))
        assertEquals(SENDER_ALIAS, sender.name)
        assertEquals(RECEIVER_AUTH_CODE, sender.authCode)
        assertEquals(SENDER_NAME, sender.realSenderName)
        assertEquals(SENDER_RELATION, sender.relation)
        assertEquals(
            RECEIVER_AUTH_CODE,
            runBlocking { receiverAuthCodeDataSource.savedCodeFlow.first() },
        )
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
        const val RECEIVER_NAME = "김수신"
        const val SENDER_ALIAS = "가족 별칭"
        const val SENDER_NAME = "이발신"
        const val SENDER_RELATION = "가족"
        const val RECEIVER_AUTH_CODE = "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
        const val MEMBER_ACCESS_TOKEN = "member-access"
        const val MEMBER_REFRESH_TOKEN = "member-refresh"
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}
