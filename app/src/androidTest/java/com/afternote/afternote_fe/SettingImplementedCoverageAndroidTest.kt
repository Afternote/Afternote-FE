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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.ui.navigation.FeatureNavigationCallbacks
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.navigation.SettingExternalActions
import com.afternote.feature.setting.presentation.navigation.SettingNavHost
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * 설정 로컬 스택을 실제 화면·Hilt ViewModel 로 띄워 본다 (#1695).
 *
 * 스택 «모양» 은 `SettingLocalNavActionsTest` 가 보므로, 여기서 재는 것은 둘이다 — 화면 클릭이 route
 * 인자를 그 화면의 ViewModel 까지 정확히 나르는가(Nav3 entry 의 키는 바깥에서 읽을 수 없어, 인자를
 * 받은 ViewModel 이 저장소에 요청한 `receiverId` 로 판정한다), 그리고 host 가 시작점 인자로 첫 화면을
 * 고르는가(entry 결선과 시작점은 JVM 테스트가 못 보는 자리라 이 클래스가 유일한 자동 가드다).
 */
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

    @Inject
    lateinit var userRepository: UserRepository

    private val fakeUserRepository: FakeUserRepository
        get() = userRepository as FakeUserRepository

    /** 로컬 스택 바닥에서 셸로 나간 횟수 — 설정 안에서 도는 동안은 0 이어야 한다. */
    private var exits = 0

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /** 루트가 `Route.Setting` 인자로 host 를 띄우는 자리를 그대로 옮긴다 — 시작점은 테스트마다 다르다. */
    private fun launchHost(startWithRecipientRegistration: Boolean = false) {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AfternoteTheme {
                    SettingNavHost(
                        navigationCallbacks = FeatureNavigationCallbacks { exits += 1 },
                        externalActions =
                            object : SettingExternalActions {
                                override fun onLogoutSuccess() = Unit

                                override fun onWithdrawSuccess() = Unit
                            },
                        startWithRecipientRegistration = startWithRecipientRegistration,
                    )
                }
            }
        }
    }

    @Test
    fun settingNavHost_startWithRecipientRegistrationOpensRegisterFirstAndBackExitsHost() {
        launchHost(startWithRecipientRegistration = true)

        // 홈 칩 진입 — 설정 홈을 거치지 않고 등록 화면이 스택 바닥이다.
        composeRule.onNodeWithText("수신자 등록").assertIsDisplayed()
        composeRule.onAllNodes(hasText("프로필 수정")).assertCountEquals(0)

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        // 바닥의 back 은 로컬 스택을 비우지 않고 셸로 나간다 — 부른 곳(홈)으로 돌아간다 (#506).
        assertEquals(1, composeRule.runOnIdle { exits })
    }

    @Test
    fun settingNavHost_receiverManageListRowNavigatesToEditWithExactReceiverId() {
        launchHost()
        waitForSettingHomeContent()
        val detailCallsBefore = fakeUserRepository.receiverDetailCalls.size
        composeRule.onAllNodes(hasText("수신자 목록")).run {
            assertCountEquals(2)
            get(1).performScrollTo().performClick()
        }
        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onAllNodes(checkboxMatcher).assertCountEquals(0)

        composeRule.onNodeWithText("김수신").performClick()

        assertEquals(RECEIVER_ID, waitForNewCall(fakeUserRepository.receiverDetailCalls, detailCallsBefore))
        assertEquals(0, exits)
    }

    @Test
    fun settingNavHost_deliveryConditionReceiverSelectionPreservesExactReceiverId() {
        launchHost()
        waitForSettingHomeContent()
        val deliveryCallsBefore = fakeUserRepository.deliveryLoadCalls.size
        val detailCallsBefore = fakeUserRepository.receiverDetailCalls.size
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

        assertEquals(RECEIVER_ID, waitForNewCall(fakeUserRepository.deliveryLoadCalls, deliveryCallsBefore))
        composeRule
            .onNodeWithText("마지막 인사말 수정하기")
            .assertIsDisplayed()
            .performClick()

        assertEquals(RECEIVER_ID, waitForNewCall(fakeUserRepository.receiverDetailCalls, detailCallsBefore))
        assertEquals(0, exits)
    }

    @Test
    fun settingNavHost_withdrawGuideCancelThenAgreementConfirmPreservesBoundary() {
        launchHost()
        waitForSettingHomeContent()
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

        // 취소는 설정 홈으로 한 칸 내려온다 — 로컬 스택 바닥을 넘어 셸로 나가지 않는다.
        waitForSettingHomeContent()
        assertEquals(0, exits)

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

        composeRule
            .onNodeWithText("안전한 탈퇴 진행을 위해 아래 문장을 입력해 주세요.")
            .assertIsDisplayed()
        assertEquals(0, exits)
    }

    private fun openWithdrawGuide() {
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("회원 탈퇴"))
        composeRule
            .onNodeWithText("회원 탈퇴")
            .performClick()
    }

    private fun waitForSettingHomeContent() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText("프로필 수정")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** [calls] 가 [sizeBefore] 보다 길어질 때까지 기다려 새로 들어온 첫 값을 돌려준다. fake 는 프로세스 싱글턴이라 이전 테스트의 기록이 남아 있다. */
    private fun waitForNewCall(
        calls: List<Long>,
        sizeBefore: Int,
    ): Long {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { calls.size > sizeBefore }
        return calls[sizeBefore]
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val RECEIVER_ID = 7L
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}
