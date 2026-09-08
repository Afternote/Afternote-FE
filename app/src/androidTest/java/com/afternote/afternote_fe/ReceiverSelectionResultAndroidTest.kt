package com.afternote.afternote_fe

import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CompletableDeferred
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
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
 * 1. `core:ui` 공용 화면이 선택된 수신자 id 목록을 완료 콜백으로 내보내고,
 * 2. app 모듈의 `popBackWithSelectedReceivers(List<Long>)` 가 그 목록을
 *    **직전 back stack entry**(에디터)의 `SavedStateHandle` 에
 *    `SELECTED_RECEIVER_IDS_KEY` (`LongArray`)로 쓰고 pop 하며,
 * 3. `feature:afternote` 에디터가 복귀 시 그 id 목록을 이름·관계로 해석해 폼에 넣는다.
 *
 * 어느 한 마디만 어긋나도 «다른 수신자가 지정되는» 회귀가 되는데, 각 모듈의 단위 테스트는
 * 자기 마디까지만 본다. 그래서 이 계약은 계측 테스트로만 단언할 수 있다.
 *
 * 소비처는 현재 애프터노트 에디터 하나다(0830 `origin/develop` 실측 — 설정은 #631 로 관리 화면이
 * 되며 공용 컴포넌트 소비를 그만두고, 타임레터·마음의 기록은 아직 각 기능 전용 구현이다).
 * 소비처가 늘어나면 각 모듈이 자기 route 테스트를 여기 옆에 더한다.
 *
 * **배리어 규약.** `navController` 의 destination 은 `popBackStack()` 순간 뒤집히지만 화면 조립은
 * 그 뒤에 따라온다. 그래서 라우트 대기만으로는 "선택 화면이 사라졌다" 를 보장하지 못한다.
 * 화면 전환 판정은 항상 [waitForEditorAddButtons] 로 한다 — "추가" 설명이 붙은 버튼은 에디터에만
 * 있고 선택 화면엔 없다. 부정 단언("들어오지 않았다") 앞에는 추가로 [awaitReceiverLoad] 로
 * 저장소를 한 바퀴 돌린다. 선택 결과 반영도 같은 저장소 홉(`resolveSelectedReceiver`)을 지나므로,
 * 그 홉에 IO 나 지연이 생겨도 단언 뒤로 착지할 수 없다.
 *
 * **진입 경로 주의.** [Route.Afternote] 의 시작 화면은 지문 로그인이고, 계측에 주입되는
 * `FakeUserProfileCacheRepository` 가 패스키 미등록(false)을 내야 홈으로 자동 통과한다.
 * 그 fake 기본값이 바뀌면 세 테스트가 모두 홈 대기에서 멈추므로, 실패 메시지에 현재 "추가" 노드
 * 개수를 실어 어느 화면에 멈췄는지 드러나게 했다([waitForAddButtons]).
 *
 * **화면 판정이 라우트가 아닌 이유.** Navigation 3 이관(#1698) 뒤 애프터노트 화면들은 피처 로컬
 * 스택에 있고 루트 destination 은 [Route.Afternote] 하나로 고정된다. 그래서 이 테스트의 화면
 * 판정은 전부 UI 앵커다 — 원래 배리어 규약이 권하던 방식과 같다.
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
     * 완료가 돌려주는 것은 **그 시점에 체크된 수신자 id 전체**다 (#1426).
     *
     * 화면은 폼에 이미 담겨 있던 수신자를 체크된 채로 열고(완료는 처음부터 활성), 완료는 체크된 id
     * 목록을 폼 수신자 전체로 되돌린다. 폼에 있던 사람은 남고, 고른 사람은 더해지고, 고르지 않은
     * 사람은 따라 들어오지 않는 것으로 판정한다.
     */
    @Test
    fun afternoteEditorNavHost_receiverSelectOpensWithFormReceiverCheckedAndConfirmAddsOnlyTheChosen() {
        receiverSource.receivers = listOf(KIM)
        openNewSocialEditor()
        waitForEditorReceiver(KIM.name)
        composeRule.onNodeWithText(PARK.name).assertDoesNotExist()

        receiverSource.receivers = listOf(KIM, PARK, LEE)
        openReceiverSelect()
        composeRule.onNodeWithText(copy(CoreUiR.string.core_ui_receiver_select_title)).assertIsDisplayed()
        waitForSelectRow(PARK.name)
        // 폼에 있던 김혜성이 체크된 채로 열리므로 완료는 고르기 전부터 활성이다.
        composeRule.onNodeWithText(confirmText).assertIsEnabled()
        composeRule.onNodeWithText(PARK.name).performClick()
        composeRule.onNodeWithText(confirmText).assertIsEnabled().performClick()

        // 선택 화면에도 세 사람이 모두 떠 있으므로, 에디터가 조립된 뒤에야 이름으로 판정할 수 있다.
        waitForEditorAddButtons()
        waitForEditorReceiver(PARK.name)
        // 이미 지정돼 있던 수신자는 유지되고, 고르지 않은 수신자는 따라 들어오지 않는다.
        composeRule.onNodeWithText(KIM.name).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(LEE.name).assertDoesNotExist()
    }

    /**
     * 화면에서 체크를 푼 수신자는 폼에서도 빠진다 — 완료는 «추가» 가 아니라 «교체» 다 (#1426).
     *
     * 체크가 0명이면 완료가 잠기므로, 이 화면으로 폼을 전부 비우는 경로는 없다는 것도 함께 고정한다.
     * 교체는 폼 상태 한 번의 쓰기라, 새로 고른 사람이 에디터에 뜬 시점엔 푼 사람도 이미 빠져 있다.
     */
    @Test
    fun afternoteEditorNavHost_receiverSelectUncheckingFormReceiverRemovesItFromEditorOnConfirm() {
        receiverSource.receivers = listOf(KIM)
        openNewSocialEditor()
        waitForEditorReceiver(KIM.name)

        receiverSource.receivers = listOf(KIM, PARK)
        openReceiverSelect()
        waitForSelectRow(PARK.name)
        composeRule.onNodeWithText(KIM.name).performClick()
        composeRule.onNodeWithText(confirmText).assertIsNotEnabled()
        composeRule.onNodeWithText(PARK.name).performClick()
        composeRule.onNodeWithText(confirmText).assertIsEnabled().performClick()

        waitForEditorAddButtons()
        waitForEditorReceiver(PARK.name)
        composeRule.onNodeWithText(KIM.name).assertDoesNotExist()
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

        // 복귀 직후 판정은 «취소가 값을 흘렸다» 를 놓칠 수 있다 — 흘린 값의 반영은 저장소를 한 번
        // 더 지나기 때문이다. 그 홉을 게이트로 붙잡았다 풀어 반드시 먼저 착지하게 만든 뒤 판정한다.
        val gate = receiverSource.gateNextLoad()
        composeRule
            .onNodeWithContentDescription(copy(CoreUiR.string.core_ui_content_description_back))
            .performClick()

        waitForEditorAddButtons()
        releaseGatedLoad(gate)

        composeRule.onNodeWithText(KIM.name).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(PARK.name).assertDoesNotExist()
    }

    /**
     * 목록을 못 그리는 상태(로딩·빈 목록·조회 실패)에서는 완료가 잠겨 있고, 재시도로 목록이
     * 살아난 뒤에야 선택이 완료를 연다.
     *
     * 빈 목록 문구는 **조회 전 초기 상태에서도** 그려진다(`SelectReceiverUiState()` 기본값이
     * `isLoading=false, receivers=[]` 라 화면 분기가 빈 목록으로 떨어진다). 그래서 "빈 목록 문구가
     * 보인다" 만으로는 조회가 일어났는지조차 알 수 없다. 조회를 게이트로 붙잡아 **로딩 표시가
     * 빈 목록 문구를 대신하고 있는 것**을 먼저 확인하고, 풀어 준 뒤에 빈 목록 문구를 단언한다.
     */
    @Test
    fun afternoteSelectReceiver_emptyAndLoadFailureBlockConfirmUntilRetryLoadsList() {
        receiverSource.receivers = emptyList()
        openNewSocialEditor()

        val emptyLoad = receiverSource.gateNextLoad()
        openReceiverSelect()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule
                .onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(emptyText).assertDoesNotExist()
        emptyLoad.barrier.complete(Unit)

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(emptyText).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(confirmText).assertIsNotEnabled()
        composeRule
            .onNodeWithContentDescription(copy(CoreUiR.string.core_ui_content_description_back))
            .performClick()
        waitForEditorAddButtons()

        receiverSource.failing = true
        openReceiverSelect()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(loadFailedText).fetchSemanticsNodes().isNotEmpty()
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

    private val emptyText: String
        get() = copy(AfternoteR.string.afternote_select_receiver_empty)

    private val loadFailedText: String
        get() = copy(AfternoteR.string.afternote_select_receiver_load_failed)

    /**
     * 애프터노트 홈의 연필 FAB 으로 계정 카테고리 에디터를 연다.
     *
     * 이관(#1698) 전에는 루트 `NavController` 로 `EditorFlowRoute` 에 곧장 뛰어들었지만, 그
     * 라우트는 이제 애프터노트 로컬 스택 안에 있어 루트에서 보이지 않는다. 대신 실제 진입점을
     * 지난다 — 홈의 FAB 은 선택된 카테고리가 없을 때 계정(`SOCIAL_NETWORK`)으로 열고, 이 테스트는
     * 카테고리를 고르지 않으므로 그 기본값이 곧 목적지다.
     *
     * 홈에서 "추가" 설명을 가진 노드는 이 FAB 하나뿐이고 에디터에는 [EDITOR_ADD_BUTTON_COUNT] 개라,
     * 개수만으로 두 화면을 구분할 수 있다.
     */
    private fun openNewSocialEditor() {
        waitForAddButtons(HOME_FAB_COUNT, "애프터노트 홈(지문 로그인 자동 통과)")
        composeRule
            .onAllNodesWithContentDescription(addDescription)
            .onFirst()
            .performClick()
        waitForEditorAddButtons()
    }

    /**
     * 수신자 지정 섹션의 추가 버튼을 눌러 선택 화면으로 간다.
     *
     * 계정 카테고리 폼에는 같은 "추가" 설명을 가진 버튼이 수신자 지정·처리 방법 두 곳에 있고,
     * 시안 순서상 수신자 지정이 앞이라 첫 번째가 대상이다(개수 단언으로 그 전제를 고정한다).
     * 순서가 바뀌면 라우트 대기가 현재 destination 을 실은 메시지로 실패한다.
     */
    private fun openReceiverSelect() {
        waitForEditorAddButtons()
        composeRule.onAllNodesWithContentDescription(addDescription).run {
            assertCountEquals(EDITOR_ADD_BUTTON_COUNT)
            get(0).performScrollTo().performClick()
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithText(copy(CoreUiR.string.core_ui_receiver_select_title))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    /**
     * 에디터가 조립됐음을 확정한다. "추가" 설명이 붙은 버튼은 에디터에만 있고 선택 화면엔 없어,
     * 수신자 이름과 달리 두 화면을 구분하는 앵커가 된다.
     */
    private fun waitForEditorAddButtons() = waitForAddButtons(EDITOR_ADD_BUTTON_COUNT, "에디터")

    /**
     * "추가" 설명을 가진 노드가 [count] 개가 될 때까지 기다린다.
     *
     * 실패 시 현재 개수를 남긴다 — 「10초 뒤 조건 미충족」만으론 어느 화면에 멈췄는지 안 드러난다.
     * 이관 뒤 루트 destination 은 그래프 host 하나로 고정이라 더는 단서가 되지 못한다.
     */
    private fun waitForAddButtons(
        count: Int,
        description: String,
    ) {
        try {
            composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
                composeRule
                    .onAllNodesWithContentDescription(addDescription)
                    .fetchSemanticsNodes()
                    .size == count
            }
        } catch (e: ComposeTimeoutException) {
            val actual =
                composeRule
                    .onAllNodesWithContentDescription(addDescription)
                    .fetchSemanticsNodes()
                    .size
            throw AssertionError(
                "$description 으로 이동하지 못했습니다. \"추가\" 노드 기대=$count 실제=$actual",
                e,
            )
        }
    }

    /**
     * 게이트에 걸린 목록 조회가 **실제로 시작된 것**을 확인하고 풀어, 저장소 왕복 한 바퀴를
     * 완주시킨다. 시작·완료 모두 직전 값보다 커졌는지로 판정한다 — 그래야 배리어 자체가
     * 무조건 참인 조건으로 무너지지 않는다.
     */
    private fun releaseGatedLoad(gate: GatedLoad) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            receiverSource.loadsStarted.get() > gate.startedBefore
        }
        val completedBefore = receiverSource.loadsCompleted.get()
        gate.barrier.complete(Unit)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            receiverSource.loadsCompleted.get() > completedBefore
        }
        composeRule.waitForIdle()
    }

    private fun waitForEditorReceiver(name: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** 선택 화면의 목록 응답이 도착해 행이 그려진 것을 확정한다 — 그 뒤에야 체크·완료 상태를 단언할 수 있다. */
    private fun waitForSelectRow(name: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private val addDescription: String
        get() = copy(AfternoteR.string.afternote_editor_content_description_add)

    /** 화면 문구는 리소스가 정본이다 — 문구가 바뀌어도 단언이 따라간다 (#567). */
    private fun copy(resId: Int): String =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(resId)

    /**
     * 조회 시점마다 응답을 갈아끼우는 수신자 목록 소스.
     *
     * [gateNextLoad] 로 다음 조회를 붙잡아 두면 로딩 상태를 관찰할 수 있고, 부정 단언 앞에
     * 저장소 왕복을 강제로 완주시키는 배리어로도 쓴다.
     */
    private class StagedReceiverSource {
        @Volatile
        var receivers: List<Receiver> = emptyList()

        @Volatile
        var failing: Boolean = false

        val loadsStarted = AtomicInteger()
        val loadsCompleted = AtomicInteger()

        @Volatile
        private var gate: CompletableDeferred<Unit>? = null

        fun gateNextLoad(): GatedLoad {
            val barrier = CompletableDeferred<Unit>()
            val startedBefore = loadsStarted.get()
            gate = barrier
            return GatedLoad(barrier = barrier, startedBefore = startedBefore)
        }

        suspend fun load(): List<Receiver> {
            loadsStarted.incrementAndGet()
            gate?.let { barrier ->
                gate = null
                barrier.await()
            }
            try {
                if (failing) throw IOException("수신자 목록 조회 실패(테스트)")
                return receivers
            } finally {
                loadsCompleted.incrementAndGet()
            }
        }
    }

    /** 붙잡아 둔 조회 한 건 — 푸는 쪽이 "언제부터"를 알아야 시작 판정이 무조건 참이 되지 않는다. */
    private class GatedLoad(
        val barrier: CompletableDeferred<Unit>,
        val startedBefore: Int,
    )

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L

        /** 계정 카테고리 에디터의 "추가" 버튼 — 수신자 지정 + 처리 방법 리스트. */
        const val EDITOR_ADD_BUTTON_COUNT = 2

        /** 애프터노트 홈의 "추가" 노드 — 연필 FAB 하나뿐이다. */
        const val HOME_FAB_COUNT = 1

        val KIM = Receiver(receiverId = 7L, name = "김수신", relation = "가족", authCode = "fake-auth-7")
        val PARK = Receiver(receiverId = 11L, name = "박친구", relation = "친구", authCode = "fake-auth-11")
        val LEE = Receiver(receiverId = 23L, name = "이지인", relation = "지인", authCode = "fake-auth-23")
    }
}
