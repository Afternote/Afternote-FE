package com.afternote.afternote_fe

import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.navigation.AppState
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import javax.inject.Inject
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.afternote.presentation.R as AfternoteR

/**
 * 공용 수신자 선택 화면과 소비 기능 사이의 **결과 전달** 계약 (#841).
 *
 * 공용 컴포넌트 자체의 상태 전이(`core:ui` [com.afternote.core.ui.receiver.ReceiverSelectScreen])는
 * 에뮬레이터 없이 도는 `ReceiverSelectScreenTest` 가 이미 고정한다. 여기서 덮는 것은 그 단위
 * 테스트가 닿지 못하는 구간 — **실제 NavHost 를 지나 소비 route 로 돌아오는 값**이다.
 *
 * 경로는 세 모듈에 걸쳐 있다.
 * 1. `core:ui` 공용 화면이 선택된 수신자 id 를 완료 콜백으로 내보내고,
 * 2. app 모듈의 `popBackWithSelectedReceiver` 가 그 id 를 **직전 back stack entry**(에디터)의
 *    `SavedStateHandle` 에 `SELECTED_RECEIVER_ID_KEY` 로 쓰고 pop 하며,
 * 3. `feature:afternote` 에디터가 복귀 시 그 id 를 이름·관계로 해석해 폼에 넣는다.
 *
 * 어느 한 마디만 어긋나도 «다른 수신자가 지정되는» 회귀가 되는데, 각 모듈의 단위 테스트는
 * 자기 마디까지만 본다. 그래서 이 계약은 계측 테스트로만 단언할 수 있다.
 *
 * 소비처는 현재 애프터노트 에디터 하나다(0830 `origin/develop` 실측 — 설정은 #631 로 관리 화면이
 * 되며 공용 컴포넌트 소비를 그만두고, 타임레터·마음의 기록은 아직 각 기능 전용 구현이다).
 * 소비처가 늘어나면 각 모듈이 자기 route 테스트를 여기 옆에 더한다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReceiverSelectionResultAndroidTest {
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

    private lateinit var navController: TestNavHostController
    private val receiverSource = StagedReceiverSource()

    @Before
    fun setUp() {
        hiltRule.inject()
        // 수신자 목록은 에디터 진입 시점과 선택 화면 진입 시점에 서로 다르게 응답해야 한다 —
        // 그래야 "선택 화면에서 새로 고른 한 명"과 "에디터가 이미 채워 둔 사람"이 구분된다.
        (userRepository as FakeUserRepository).onGetReceivers = { receiverSource.load() }

        composeRule.activityRule.scenario.onActivity { activity ->
            navController =
                TestNavHostController(activity).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }
            activity.setContent {
                AfternoteTheme {
                    AppNavigation(
                        startDestination = Route.Afternote,
                        appState = AppState(navController),
                    )
                }
            }
        }
    }

    /**
     * 완료가 돌려주는 것은 **고른 그 한 명의 id** 다.
     *
     * 목록 순서(index)나 목록 전체가 아니라 id 하나가 건너가는지를, 고르지 않은 수신자가 폼에
     * 들어오지 않는 것으로 판정한다.
     */
    @Test
    fun afternoteEditorNavHost_receiverSelectConfirmAddsOnlyTheChosenReceiverToEditorForm() {
        receiverSource.receivers = listOf(KIM)
        openNewSocialEditor()
        waitForEditorReceiver(KIM.name)
        composeRule.onNodeWithText(PARK.name).assertDoesNotExist()

        receiverSource.receivers = listOf(KIM, PARK, LEE)
        openReceiverSelect()
        composeRule.onNodeWithText(copy(CoreUiR.string.core_ui_receiver_select_title)).assertIsDisplayed()
        composeRule.onNodeWithText(confirmText).assertIsNotEnabled()
        composeRule.onNodeWithText(PARK.name).performClick()
        composeRule.onNodeWithText(confirmText).assertIsEnabled().performClick()

        waitForRoute<AfternoteRoute.EditorRoute>()
        waitForEditorReceiver(PARK.name)
        // 이미 지정돼 있던 수신자는 유지되고, 고르지 않은 수신자는 따라 들어오지 않는다.
        composeRule.onNodeWithText(KIM.name).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(LEE.name).assertDoesNotExist()
    }

    /** 취소(뒤로가기)는 화면 안에서 고른 값을 버리고 소비 화면의 기존 지정을 그대로 둔다. */
    @Test
    fun afternoteEditorNavHost_receiverSelectBackKeepsEditorReceiversUnchanged() {
        receiverSource.receivers = listOf(KIM)
        openNewSocialEditor()
        waitForEditorReceiver(KIM.name)

        receiverSource.receivers = listOf(KIM, PARK)
        openReceiverSelect()
        composeRule.onNodeWithText(PARK.name).performClick()
        composeRule
            .onNodeWithContentDescription(copy(CoreUiR.string.core_ui_content_description_back))
            .performClick()

        waitForRoute<AfternoteRoute.EditorRoute>()
        waitForEditorReceiver(KIM.name)
        composeRule.onNodeWithText(PARK.name).assertDoesNotExist()
    }

    /**
     * 목록을 못 그리는 상태(빈 목록·조회 실패)에서는 완료가 잠겨 있고, 재시도로 목록이 살아난 뒤에야
     * 선택이 완료를 연다.
     *
     * 상태 화면은 소비 기능이 소유하고(`listReplacement`) 완료 버튼은 공용 컴포넌트가 소유한다 —
     * 둘이 실제로 한 화면에 조립됐을 때의 조합은 여기서만 드러난다.
     */
    @Test
    fun afternoteSelectReceiver_emptyAndLoadFailureBlockConfirmUntilRetryLoadsList() {
        receiverSource.receivers = emptyList()
        openNewSocialEditor()

        openReceiverSelect()
        composeRule.onNodeWithText(copy(AfternoteR.string.afternote_select_receiver_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(confirmText).assertIsNotEnabled()
        composeRule
            .onNodeWithContentDescription(copy(CoreUiR.string.core_ui_content_description_back))
            .performClick()
        waitForRoute<AfternoteRoute.EditorRoute>()

        receiverSource.failing = true
        openReceiverSelect()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithText(copy(AfternoteR.string.afternote_select_receiver_load_failed))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(confirmText).assertIsNotEnabled()

        receiverSource.failing = false
        receiverSource.receivers = listOf(KIM, PARK)
        composeRule.onNodeWithText(copy(AfternoteR.string.afternote_select_receiver_retry)).performClick()

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(KIM.name).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(confirmText).assertIsNotEnabled()
        composeRule.onNodeWithText(PARK.name).performClick()
        composeRule.onNodeWithText(confirmText).assertIsEnabled()
    }

    private val confirmText: String
        get() = copy(CoreUiR.string.core_ui_receiver_select_confirm)

    private fun openNewSocialEditor() {
        waitForRoute<AfternoteRoute.AfternoteHomeRoute>()
        composeRule.runOnIdle {
            navController.navigate(
                AfternoteRoute.EditorFlowRoute(initialType = AfternoteType.SOCIAL_NETWORK),
            )
        }
        waitForRoute<AfternoteRoute.EditorRoute>()
        waitForEditorAddButtons()
    }

    /**
     * 수신자 지정 섹션의 추가 버튼을 눌러 선택 화면으로 간다.
     *
     * 계정 카테고리 폼에는 같은 "추가" 설명을 가진 버튼이 수신자 지정·처리 방법 두 곳에 있고,
     * 시안 순서상 수신자 지정이 앞이라 첫 번째가 대상이다. 순서가 바뀌면 라우트 대기에서 즉시 실패한다.
     */
    private fun openReceiverSelect() {
        waitForEditorAddButtons()
        composeRule.onAllNodesWithContentDescription(addDescription()).run {
            assertCountEquals(EDITOR_ADD_BUTTON_COUNT)
            get(0).performScrollTo().performClick()
        }
        waitForRoute<AfternoteRoute.SelectReceiverRoute>()
    }

    private fun waitForEditorAddButtons() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithContentDescription(addDescription())
                .fetchSemanticsNodes()
                .size == EDITOR_ADD_BUTTON_COUNT
        }
    }

    private fun waitForEditorReceiver(name: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private inline fun <reified T : Any> waitForRoute() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            navController.currentDestination?.hasRoute<T>() == true
        }
    }

    private fun addDescription(): String = copy(AfternoteR.string.afternote_editor_content_description_add)

    /** 화면 문구는 리소스가 정본이다 — 문구가 바뀌어도 단언이 따라간다 (#567). */
    private fun copy(resId: Int): String =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(resId)

    /** 조회 시점마다 응답을 갈아끼우는 수신자 목록 소스. */
    private class StagedReceiverSource {
        @Volatile
        var receivers: List<Receiver> = emptyList()

        @Volatile
        var failing: Boolean = false

        fun load(): List<Receiver> {
            if (failing) throw IOException("수신자 목록 조회 실패(테스트)")
            return receivers
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L

        /** 계정 카테고리 에디터의 "추가" 버튼 — 수신자 지정 + 처리 방법 리스트. */
        const val EDITOR_ADD_BUTTON_COUNT = 2

        val KIM = Receiver(receiverId = 7L, name = "김수신", relation = "가족", authCode = "fake-auth-7")
        val PARK = Receiver(receiverId = 11L, name = "박친구", relation = "친구", authCode = "fake-auth-11")
        val LEE = Receiver(receiverId = 23L, name = "이지인", relation = "지인", authCode = "fake-auth-23")
    }
}
