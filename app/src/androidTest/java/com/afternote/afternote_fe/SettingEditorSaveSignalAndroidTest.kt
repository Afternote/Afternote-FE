package com.afternote.afternote_fe

import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.ReceiverDetail
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
import java.io.IOException
import javax.inject.Inject

/**
 * 설정 편집 화면의 저장 신호를 실제 로컬 스택에서 본다 (#2179).
 *
 * reducer 가 세운 완료 신호(수신자 수정의 `pendingEvent`, 전달 조건의 `isSaved`)가 화면에서 한 번만
 * 소비되어 스택을 정확히 한 칸 내리는지, 실패와 입력 검증은 화면을 닫지 않는지를 잰다. 신호 소비가
 * 두 번 일어나면 한 칸이 아니라 설정 홈까지 내려가고, 소비가 안 되면 편집 화면에 남는다. 둘 다
 * ViewModel 단위 테스트로는 보이지 않는 자리다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingEditorSaveSignalAndroidTest {
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
        fakeUserRepository.receiverDetails[RECEIVER_ID] =
            ReceiverDetail(
                receiverId = RECEIVER_ID,
                name = RECEIVER_NAME,
                relation = "가족",
                phone = "01012345678",
                email = "kim@afternote.local",
                dailyQuestionCount = 0,
                timeLetterCount = 0,
                afterNoteCount = 0,
                message = "잘 지내",
            )
        fakeUserRepository.onGetReceiverDetail = null
    }

    @Test
    fun receiverEdit_saveSuccessPopsExactlyOneScreenToReceiverList() {
        fakeUserRepository.onUpdateReceiver = null
        fakeUserRepository.onUpdateReceiverMessage = null
        openReceiverEdit()
        val updatesBefore = fakeUserRepository.receiverUpdateCalls.size
        val messagesBefore = fakeUserRepository.receiverMessageCalls.size

        composeRule.onNode(hasSetTextAction() and hasText(RECEIVER_NAME)).performTextReplacement("김수정")
        composeRule.onNodeWithText(SAVE_ACTION).assertIsEnabled().performClick()

        waitUntilGone(EDIT_TITLE)
        // 한 칸만 내려와 목록이 보인다 — 신호가 두 번 소비되면 설정 홈까지 내려간다.
        composeRule.onNodeWithText("김수정").assertIsDisplayed()
        composeRule.onAllNodes(hasText("프로필 수정")).assertCountEquals(0)
        assertEquals(1, fakeUserRepository.receiverUpdateCalls.size - updatesBefore)
        assertEquals(1, fakeUserRepository.receiverMessageCalls.size - messagesBefore)
        assertEquals(0, exits)
    }

    @Test
    fun receiverEdit_saveFailureShowsMessageAndStaysOnEdit() {
        fakeUserRepository.onUpdateReceiver = { _, _, _, _, _ -> throw IOException("offline") }
        openReceiverEdit()
        val updatesBefore = fakeUserRepository.receiverUpdateCalls.size
        val messagesBefore = fakeUserRepository.receiverMessageCalls.size

        composeRule.onNodeWithText(SAVE_ACTION).performClick()

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.runOnIdle { fakeUserRepository.receiverUpdateCalls.size > updatesBefore }
        }
        // 오류는 LazyColumn의 마지막 항목이므로 작은 화면에서도 스크롤해 실제 표시를 확인한다.
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(EDIT_FAILED_MESSAGE))
        composeRule.onNodeWithText(EDIT_FAILED_MESSAGE).assertIsDisplayed()
        // 실패는 화면을 닫지 않고, 저장 중 잠금도 풀려 다시 누를 수 있다.
        composeRule.onNodeWithText(EDIT_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(SAVE_ACTION).assertIsEnabled()
        assertEquals(1, fakeUserRepository.receiverUpdateCalls.size - updatesBefore)
        assertEquals(0, fakeUserRepository.receiverMessageCalls.size - messagesBefore)
        assertEquals(0, exits)
    }

    @Test
    fun receiverEdit_invalidEmailOrPhoneBlocksSaveWithoutRequest() {
        openReceiverEdit()
        val updatesBefore = fakeUserRepository.receiverUpdateCalls.size

        composeRule.onNode(hasSetTextAction() and hasText("kim@afternote.local")).performTextReplacement("kim@afternote")
        composeRule.onNodeWithText("올바른 이메일 주소를 입력해주세요.").assertIsDisplayed()
        composeRule.onNodeWithText(SAVE_ACTION).assertIsNotEnabled().performClick()

        composeRule.onNode(hasSetTextAction() and hasText("kim@afternote")).performTextReplacement("kim@afternote.local")
        composeRule.onNode(hasSetTextAction() and hasText("010", substring = true)).performTextReplacement("0101234")
        composeRule.onNodeWithText("올바른 연락처를 입력해주세요.").assertIsDisplayed()
        composeRule.onNodeWithText(SAVE_ACTION).assertIsNotEnabled().performClick()

        composeRule.onNodeWithText(EDIT_TITLE).assertIsDisplayed()
        assertEquals(0, composeRule.runOnIdle { fakeUserRepository.receiverUpdateCalls.size - updatesBefore })
        assertEquals(0, exits)
    }

    @Test
    fun deliveryCondition_saveSuccessPopsExactlyOneScreenToReceiverSelection() {
        fakeUserRepository.onGetReceiverDeliveryConditions = null
        fakeUserRepository.onUpdateReceiverDeliveryConditions = null
        launchHost()
        waitForSettingHomeContent()
        val updatesBefore = fakeUserRepository.deliveryUpdateCalls.size
        composeRule.onNodeWithText("사후 전달 조건").performScrollTo().performClick()
        composeRule.onAllNodes(checkboxMatcher).run {
            assertCountEquals(1)
            get(0).performClick()
        }
        composeRule.onNodeWithText(RECEIVER_SELECT_DONE).performClick()

        // 조회가 끝나야 저장이 열린다.
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(DELIVERY_SAVE) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(DELIVERY_SAVE).performClick()

        waitUntilShown(RECEIVER_SELECT_DONE)
        // 한 칸만 내려와 수신자 선택이 보인다 — 신호가 두 번 소비되면 설정 홈까지 내려간다.
        composeRule.onAllNodes(hasText("프로필 수정")).assertCountEquals(0)
        assertEquals(1, fakeUserRepository.deliveryUpdateCalls.size - updatesBefore)
        assertEquals(RECEIVER_ID, fakeUserRepository.deliveryUpdateCalls[updatesBefore].receiverId)
        assertEquals(0, exits)
    }

    /** 설정 홈 → 수신자 목록 → 김수신 행으로 수정 화면을 연다. 프리필이 끝나야 폼이 그려진다. */
    private fun openReceiverEdit() {
        launchHost()
        waitForSettingHomeContent()
        composeRule.onAllNodes(hasText("수신자 목록")).run {
            assertCountEquals(2)
            get(1).performScrollTo().performClick()
        }
        composeRule.onNodeWithText(RECEIVER_NAME).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasSetTextAction() and hasText(RECEIVER_NAME)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun launchHost() {
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
                        startWithRecipientRegistration = false,
                    )
                }
            }
        }
    }

    private fun waitForSettingHomeContent() = waitUntilShown("프로필 수정")

    private fun waitUntilShown(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntilGone(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val RECEIVER_ID = 7L
        const val RECEIVER_NAME = "김수신"
        const val EDIT_TITLE = "수신자 수정"
        const val SAVE_ACTION = "수정"
        const val EDIT_FAILED_MESSAGE = "수신자 수정에 실패했습니다. 잠시 후 다시 시도해주세요."
        const val DELIVERY_SAVE = "저장"
        const val RECEIVER_SELECT_DONE = "수신자 선택 완료하기"
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}
