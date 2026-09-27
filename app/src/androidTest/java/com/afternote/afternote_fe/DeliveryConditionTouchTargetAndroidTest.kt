package com.afternote.afternote_fe

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.Session
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.ui.testing.EnabledClickTarget
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.scanEnabledClickTargets
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.io.FileInputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.setting.presentation.R as SettingR

/**
 * 실제 설정 화면의 마지막 인사말 편집 행이 48dp 터치 계약을 지키는지 잰다 (#2035).
 *
 * 화면을 따로 세우지 않는다. 실제 [MainActivity] 가 실제 앱을 띄우고, 계측이 이미 주입해 둔
 * fake 로 홈에서 설정, 사후 전달 조건까지 공개 UI 로만 걸어 들어간다. 판정 기준은
 * `scanEnabledClickTargets()` 가 읽는 `touchBoundsInRoot` 다. Foundation 이 hit-test 에 실제로
 * 적용한 최소 영역 확장과 ancestor clipping 이 둘 다 반영된 값이라, 보이는 행 높이가 아니라
 * 손가락이 닿는 영역을 잰다.
 *
 * 뷰포트는 기기 기본값에 맡기지 않는다. [ViewportOverrideRule] 이 Activity 기동 전에
 * `wm size`, `wm density`, `font_scale` 을 덮어 360x800dp 기본 글자와 320x568dp 두 배 글자를
 * 고정하고, 테스트가 끝나면 원래 override 를 그대로 되돌린다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DeliveryConditionTouchTargetAndroidTest {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    private val fakeUser get() = userRepository as FakeUserRepository

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val viewportRule = ViewportOverrideRule()

    @get:Rule(order = 2)
    val signedInRule = SignedInBeforeLaunchRule(hiltRule) { prepareSignedInSession() }

    @get:Rule(order = 3)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 4)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    @ViewportOverride(widthDp = 360, heightDp = 800, fontScale = 1.0f)
    fun regularViewport_lastGreetingRowKeepsMinimumTouchAreaAndAcceptsTouchOutsideVisualRow() {
        openDeliveryCondition()
        assertRenderedViewport()

        composeRule.onNodeWithText(lastGreetingLabel).assertIsDisplayed()
        val target = lastGreetingTarget()
        logMeasurement("regular-viewport", target)
        assertMinimumTouchArea(target)
        assertTouchBoundsInsideViewport(target)

        tapOutsideVisualRow("regular-viewport", target)
        assertRecipientEditOpenedForSelectedReceiver()
    }

    @Test
    @ViewportOverride(widthDp = 320, heightDp = 568, fontScale = 2.0f)
    fun smallViewportWithDoubleFontScale_scrolledLastGreetingRowKeepsMinimumTouchAreaInsideViewport() {
        openDeliveryCondition()
        assertRenderedViewport()

        val beforeScrollOffset = verticalScrollOffset()
        val beforeScroll = lastGreetingTarget()
        logMeasurement("small-viewport-before-scroll", beforeScroll, "scrollOffset=$beforeScrollOffset")

        // 스크롤 전에는 행이 뷰포트 아래에 있어 ancestor clip 이 hit 영역을 통째로 잘라낸다.
        // 잘린 rect 는 좌표가 0,0 으로 접히므로 top 값을 스크롤 뒤와 비교할 수 없다. 대신
        // 잘려서 누를 곳이 없다는 상태 자체를 단언한다.
        assertTrue(
            "작은 화면 두 배 글자에서 마지막 인사말 행이 스크롤 전부터 보입니다. ${describe(beforeScroll)}",
            beforeScroll.isClippedAway(),
        )
        composeRule.onNodeWithText(lastGreetingLabel).assertIsNotDisplayed()

        // 마지막 안내 문구까지 내려 행을 뷰포트 안쪽에 둔다. 행 자체로 스크롤하면 행 아래 끝이
        // 뷰포트 하단에 딱 붙어, 아래로 확장된 터치 영역이 스크롤 컨테이너 clip 에 잘린다.
        deliveryScrollContainer()
            .performScrollToNode(hasText(context.getString(SettingR.string.setting_after_delivery_notice_safety)))

        val afterScrollOffset = verticalScrollOffset()
        val afterScroll = lastGreetingTarget()
        logMeasurement("small-viewport-after-scroll", afterScroll, "scrollOffset=$afterScrollOffset")

        assertTrue(
            "작은 화면 두 배 글자에서 스크롤이 일어나지 않았습니다. " +
                "scrollOffset $beforeScrollOffset -> $afterScrollOffset",
            afterScrollOffset > beforeScrollOffset,
        )
        composeRule.onNodeWithText(lastGreetingLabel).assertIsDisplayed()
        assertMinimumTouchArea(afterScroll)
        assertTouchBoundsInsideViewport(afterScroll)

        tapOutsideVisualRow("small-viewport-after-scroll", afterScroll)
        assertRecipientEditOpenedForSelectedReceiver()
    }

    /**
     * Activity 기동 전에 실행된다. 로그인 UI 를 거치지 않고 실제 [AuthRepository] 에 fake 세션을
     * 저장해 실제 앱이 홈부터 시작하게 한다.
     */
    private fun prepareSignedInSession() {
        val session = Session.DefaultSession(FAKE_ACCESS_TOKEN, FAKE_REFRESH_TOKEN)
        runBlocking {
            authRepository.saveSession(session.accessToken, session.refreshToken)
        }
        // 전달 조건 조회는 이 시나리오의 관심사가 아니다. 빈 목록 성공으로 닫아 화면 상단에
        // 오류 문구가 끼어들지 않게 한다.
        fakeUser.onGetReceiverDeliveryConditions = { receiverId ->
            ReceiverDeliveryConditions(receiverId = receiverId, conditions = emptyList())
        }
        // 수신자별로 다른 이름을 돌려준다. 편집 화면에 뜬 이름만으로 어느 수신자가 실려 갔는지
        // 갈리므로, 내부 라우트를 들여다보지 않고 콜백이 실제로 탄 경로를 판정할 수 있다.
        fakeUser.onGetReceiverDetail = { receiverId -> receiverDetailFor(receiverId) }
    }

    private fun receiverDetailFor(receiverId: Long): ReceiverDetail =
        ReceiverDetail(
            receiverId = receiverId,
            name = if (receiverId == SELECTED_RECEIVER_ID) SELECTED_RECEIVER_NAME else OTHER_RECEIVER_NAME,
            relation = "가족",
            phone = "01012345678",
            email = "kim@afternote.local",
            dailyQuestionCount = 0,
            timeLetterCount = 0,
            afterNoteCount = 0,
            message = null,
            authCode = "fake-auth-$receiverId",
        )

    /** 실제 앱 UI 만으로 홈에서 사후 전달 조건 화면까지 들어간다. */
    private fun openDeliveryCondition() {
        val settingLabel = context.getString(CoreUiR.string.core_ui_home_top_bar_setting)
        composeRule.waitUntilAtLeastOneExists(hasContentDescription(settingLabel), TIMEOUT_MILLIS)
        composeRule.onNodeWithContentDescription(settingLabel).performClick()

        composeRule.waitUntilAtLeastOneExists(
            hasText(context.getString(SettingR.string.setting_account_profile_edit)),
            TIMEOUT_MILLIS,
        )
        val deliveryLabel = context.getString(SettingR.string.setting_recipient_after_delivery)
        composeRule.onNodeWithText(deliveryLabel).performScrollTo().performClick()

        composeRule.waitUntilAtLeastOneExists(hasText(SELECTED_RECEIVER_NAME), TIMEOUT_MILLIS)
        // 계측 fake 의 수신자는 SELECTED_RECEIVER_ID 하나뿐이다. 체크박스가 하나라는 것을 먼저
        // 못박아 두면, 아래에서 고른 첫 번째가 곧 그 수신자라는 전제가 드러난다.
        composeRule
            .onAllNodes(checkboxMatcher)
            .assertCountEquals(1)
            .onFirst()
            .performClick()
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.core_ui_receiver_select_confirm))
            .performClick()

        composeRule.waitUntilAtLeastOneExists(hasText(lastGreetingLabel), TIMEOUT_MILLIS)
        // 전환 애니메이션이 끝난 배치에서 재도록 한 번 더 idle 을 기다린다.
        composeRule.waitForIdle()
    }

    private val lastGreetingLabel: String
        get() = context.getString(SettingR.string.setting_last_greeting_edit_section_title)

    private fun lastGreetingTarget(): EnabledClickTarget {
        val targets = composeRule.scanEnabledClickTargets()
        return targets.singleOrNull { it.name.contains(lastGreetingLabel) }
            ?: throw AssertionError(
                "마지막 인사말 편집 행을 하나로 특정하지 못했습니다. " +
                    "수집된 클릭 타깃: ${targets.joinToString { it.name.ifBlank { "(이름 없음)" } }}",
            )
    }

    /** 실제 렌더 시점의 density, fontScale, 표시 크기가 이 테스트가 요구한 뷰포트인지 본다. */
    private fun assertRenderedViewport() {
        val spec = viewportRule.appliedSpec()
        val metrics = realDisplayMetrics()
        assertEquals("표시 가로(px)", spec.widthDp * VIEWPORT_PX_PER_DP, metrics.widthPixels)
        assertEquals("표시 세로(px)", spec.heightDp * VIEWPORT_PX_PER_DP, metrics.heightPixels)
        assertEquals("표시 densityDpi", VIEWPORT_DENSITY_DPI, metrics.densityDpi)

        val root = composeRule.onRoot().fetchSemanticsNode()
        val density = root.layoutInfo.density
        assertEquals("컴포지션 density", VIEWPORT_PX_PER_DP.toFloat(), density.density, DENSITY_TOLERANCE)
        assertEquals("컴포지션 fontScale", spec.fontScale, density.fontScale, DENSITY_TOLERANCE)

        val rootWidthDp = with(density) { root.size.width.toDp() }
        val rootHeightDp = with(density) { root.size.height.toDp() }
        logLine(
            "viewport spec=${spec.widthDp}x${spec.heightDp}dp fontScale=${spec.fontScale} " +
                "display=${metrics.widthPixels}x${metrics.heightPixels}px@${metrics.densityDpi}dpi " +
                "root=${rootWidthDp.value}x${rootHeightDp.value}dp",
        )
        assertEquals("컴포즈 루트 가로(dp)", spec.widthDp.toFloat(), rootWidthDp.value, DP_TOLERANCE)
        assertTrue(
            "컴포즈 루트 세로(${rootHeightDp.value}dp)가 뷰포트(${spec.heightDp}dp)를 넘습니다",
            rootHeightDp.value <= spec.heightDp + DP_TOLERANCE,
        )
    }

    private fun assertMinimumTouchArea(target: EnabledClickTarget) {
        assertTrue(
            "마지막 인사말 편집 행의 실제 터치 영역이 $MinimumTouchTargetSize 미만입니다. ${describe(target)}",
            !target.isSmallerThan(MinimumTouchTargetSize),
        )
    }

    /** ancestor clipping 이 반영된 터치 영역이 화면 밖으로 새지 않는지 본다. */
    private fun assertTouchBoundsInsideViewport(target: EnabledClickTarget) {
        val root = composeRule.onRoot().fetchSemanticsNode()
        val density = root.layoutInfo.density
        val rootWidth = with(density) { root.size.width.toDp() }
        val rootHeight = with(density) { root.size.height.toDp() }
        val bounds = target.touchBounds
        assertTrue(
            "터치 영역이 뷰포트(${rootWidth.value}x${rootHeight.value}dp)를 벗어납니다. ${describe(target)}",
            bounds.left >= (-DP_TOLERANCE).dp &&
                bounds.top >= (-DP_TOLERANCE).dp &&
                bounds.right <= rootWidth + DP_TOLERANCE.dp &&
                bounds.bottom <= rootHeight + DP_TOLERANCE.dp,
        )
    }

    /**
     * 보이는 행 밖, 확장된 터치 영역 안의 좌표를 실제 터치로 누른다.
     *
     * 시맨틱 `performClick()` 은 노드의 액션을 직접 호출해 hit-test 를 건너뛴다. 여기서 재려는
     * 것이 바로 그 hit-test 라서, 루트 좌표로 MotionEvent 를 넣어 Foundation 이 실제로 이 행을
     * 집는지 본다.
     */
    private fun tapOutsideVisualRow(
        label: String,
        target: EnabledClickTarget,
    ) {
        val gapAbove = target.layoutBounds.top - target.touchBounds.top
        val gapBelow = target.touchBounds.bottom - target.layoutBounds.bottom
        val tapY =
            when {
                gapAbove >= MIN_EXTERNAL_GAP -> {
                    target.touchBounds.top + gapAbove / 2f
                }

                gapBelow >= MIN_EXTERNAL_GAP -> {
                    target.layoutBounds.bottom + gapBelow / 2f
                }

                else -> {
                    throw AssertionError(
                        "보이는 행 밖이면서 터치 영역 안인 좌표가 없습니다. ${describe(target)}",
                    )
                }
            }
        val tapX = target.touchBounds.left + (target.touchBounds.right - target.touchBounds.left) / 2f

        assertTrue(
            "탭 좌표 $tapY 가 보이는 행 안입니다. ${describe(target)}",
            tapY < target.layoutBounds.top || tapY > target.layoutBounds.bottom,
        )
        assertTrue(
            "탭 좌표 $tapY 가 터치 영역 밖입니다. ${describe(target)}",
            tapY > target.touchBounds.top && tapY < target.touchBounds.bottom,
        )

        val root = composeRule.onRoot().fetchSemanticsNode()
        val density = root.layoutInfo.density
        // touchBoundsInRoot 는 루트 기준 px 다. performTouchInput 의 좌표계는 대상 노드의
        // 좌상단 기준이라, 루트 노드를 잡고 그 원점을 빼 두 좌표계를 맞춘다.
        val rootOrigin = root.boundsInRoot.topLeft
        val tapInRoot = with(density) { Offset(tapX.toPx(), tapY.toPx()) }
        val tapInNode = tapInRoot - rootOrigin
        logLine(
            "$label tap=(${tapX.value}dp, ${tapY.value}dp) rootPx=(${tapInRoot.x}, ${tapInRoot.y}) " +
                "rootOriginPx=(${rootOrigin.x}, ${rootOrigin.y})",
        )

        composeRule.onRoot().performTouchInput { click(tapInNode) }
    }

    /** 터치가 실제 마지막 인사말 콜백을 태웠는지, 열린 화면과 그 화면이 실은 수신자로 본다. */
    private fun assertRecipientEditOpenedForSelectedReceiver() {
        composeRule.waitUntilAtLeastOneExists(hasText(RECIPIENT_EDIT_TITLE), TIMEOUT_MILLIS)
        // 편집 화면 본문은 LazyColumn 이라 화면 밖 항목은 아직 합성되지 않는다.
        composeRule
            .onAllNodes(hasScrollAction())
            .onFirst()
            .performScrollToNode(hasText(SELECTED_RECEIVER_NAME))
        composeRule.onNodeWithText(SELECTED_RECEIVER_NAME).assertIsDisplayed()
    }

    /** 마지막 인사말 행을 담은 스크롤 컨테이너. 전 화면이 아직 트리에 남아 있어도 헷갈리지 않는다. */
    private fun deliveryScrollContainer(): SemanticsNodeInteraction =
        composeRule.onNode(hasScrollAction() and hasAnyDescendant(hasText(lastGreetingLabel)))

    private fun verticalScrollOffset(): Float {
        val range =
            deliveryScrollContainer()
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.VerticalScrollAxisRange)
                ?: throw AssertionError("사후 전달 조건 화면에서 세로 스크롤 상태를 읽지 못했습니다")
        return range.value()
    }

    private fun logMeasurement(
        label: String,
        target: EnabledClickTarget,
        extra: String = "",
    ) {
        logLine("$label ${describe(target)}${if (extra.isBlank()) "" else " $extra"}")
    }

    private fun describe(target: EnabledClickTarget): String =
        "name=\"${target.name}\" " +
            "layout=${target.layoutWidth.value}x${target.layoutHeight.value}dp" +
            "@(${target.layoutBounds.left.value}, ${target.layoutBounds.top.value}) " +
            "touch=${target.width.value}x${target.height.value}dp" +
            "@(${target.touchBounds.left.value}, ${target.touchBounds.top.value})"

    private fun logLine(message: String) {
        Log.i(TAG, message)
        println("$TAG $message")
    }

    private companion object {
        const val TAG = "DeliveryConditionTouch"
        const val TIMEOUT_MILLIS = 10_000L
        const val FAKE_ACCESS_TOKEN = "touch-target-access"
        const val FAKE_REFRESH_TOKEN = "touch-target-refresh"
        const val SELECTED_RECEIVER_ID = 7L
        const val SELECTED_RECEIVER_NAME = "김수신"
        const val OTHER_RECEIVER_NAME = "다른수신자"
        const val RECIPIENT_EDIT_TITLE = "수신자 수정"
        const val DENSITY_TOLERANCE = 0.01f
        const val DP_TOLERANCE = 1f
        val MIN_EXTERNAL_GAP = 2.dp
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}

/** [ViewportOverrideRule] 이 Activity 기동 전에 고정할 화면 크기와 글자 배율. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ViewportOverride(
    val widthDp: Int,
    val heightDp: Int,
    val fontScale: Float,
)

/**
 * Hilt 주입과 기동 전 준비를 Activity 실행보다 먼저 끝낸다.
 *
 * `createAndroidComposeRule` 보다 바깥 order 로 걸어야 의미가 있다. 안쪽에 두면 Activity 가 이미
 * 뜬 뒤라 진입 화면이 온보딩으로 정해진다.
 */
class SignedInBeforeLaunchRule(
    private val hiltRule: HiltAndroidRule,
    private val prepare: () -> Unit,
) : ExternalResource() {
    override fun before() {
        hiltRule.inject()
        prepare()
    }
}

/**
 * [ViewportOverride] 가 붙은 테스트에만 화면 크기, 밀도, 글자 배율을 덮어씌운다.
 *
 * 기기 기본값에 맡기면 같은 테스트가 기기마다 다른 폭에서 재므로 48dp 판정이 흔들린다.
 * `wm size` 와 `wm density` 는 px 와 dpi 를 받으므로, dp 는 [VIEWPORT_PX_PER_DP] 로 환산한다.
 * 기기를 만지기 전에 직전 상태를 먼저 읽어 두고, 적용이 도중에 실패해도 그 상태로 되돌린다.
 * 복원이 확인되지 않으면 테스트를 실패시킨다. 뒤이어 도는 CI 테스트가 바뀐 뷰포트에서 조용히
 * 돌면, 그쪽 실패의 원인이 여기라는 것을 아무도 모른다.
 */
class ViewportOverrideRule : TestRule {
    private var currentSpec: ViewportOverride? = null

    fun appliedSpec(): ViewportOverride = requireNotNull(currentSpec) { "@ViewportOverride 가 붙지 않은 테스트입니다" }

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement {
        val spec = description.getAnnotation(ViewportOverride::class.java) ?: return base
        return object : Statement() {
            override fun evaluate() {
                // 기기를 만지기 전에 읽는다. 적용이 중간에 깨져도 되돌릴 기준이 남는다.
                val original = capture()
                var failure: Throwable? = null
                try {
                    currentSpec = spec
                    applyViewport(spec)
                    base.evaluate()
                } catch (throwable: Throwable) {
                    failure = throwable
                } finally {
                    currentSpec = null
                }

                val restoreFailure = runCatching { restore(original) }.exceptionOrNull()
                // 본 실패가 있으면 그것을 던지고 복원 실패는 suppressed 로 붙인다. 복원만 실패했으면
                // 그 자체로 테스트를 떨어뜨린다. 복원 실패를 통과로 삼키지 않는다.
                failure?.let { primary ->
                    restoreFailure?.let(primary::addSuppressed)
                    throw primary
                }
                restoreFailure?.let { throw it }
            }
        }
    }

    /** 기기를 건드리기 전 상태. override 문자열과 그때 실제로 적용돼 있던 값을 함께 남긴다. */
    private fun capture(): ViewportState {
        val metrics = realDisplayMetrics()
        return ViewportState(
            sizeOverride = overrideValue(shell("wm size"), "Override size:"),
            densityOverride = overrideValue(shell("wm density"), "Override density:"),
            fontScaleSetting =
                shell("settings get system font_scale")
                    .trim()
                    .takeUnless { it.isBlank() || it == "null" },
            widthPx = metrics.widthPixels,
            heightPx = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            fontScale = systemFontScale(),
        )
    }

    private fun applyViewport(spec: ViewportOverride) {
        val widthPx = spec.widthDp * VIEWPORT_PX_PER_DP
        val heightPx = spec.heightDp * VIEWPORT_PX_PER_DP
        shell("wm size ${widthPx}x$heightPx")
        shell("wm density $VIEWPORT_DENSITY_DPI")
        shell("settings put system font_scale ${spec.fontScale}")
        awaitViewport(widthPx, heightPx, spec.fontScale)
    }

    /**
     * 직전 상태로 되돌린다. override 가 없었으면 `reset`, `font_scale` 설정이 없었으면 `delete` 다.
     *
     * 확인은 명령의 성공이 아니라 실제 적용값으로 한다. 안드로이드는 `font_scale` 설정이 없으면
     * 1.0 으로 정규화하므로, 설정이 없었다는 사실이 아니라 그때 실제로 쓰이던 배율이 돌아왔는지를
     * 본다.
     */
    private fun restore(original: ViewportState) {
        val failures = mutableListOf<Throwable>()
        val restoreFont = {
            shell(
                original.fontScaleSetting
                    ?.let { "settings put system font_scale $it" }
                    ?: "settings delete system font_scale",
            )
        }
        val restoreDensity = { shell(original.densityOverride?.let { "wm density $it" } ?: "wm density reset") }
        val restoreSize = { shell(original.sizeOverride?.let { "wm size $it" } ?: "wm size reset") }
        // 하나가 실패해도 나머지 복원을 건너뛰지 않는다.
        listOf(restoreFont, restoreDensity, restoreSize).forEach { command ->
            runCatching { command() }.onFailure { failures += it }
        }

        val restored =
            awaitCondition {
                val metrics = realDisplayMetrics()
                metrics.widthPixels == original.widthPx &&
                    metrics.heightPixels == original.heightPx &&
                    metrics.densityDpi == original.densityDpi &&
                    abs(systemFontScale() - original.fontScale) < FONT_SCALE_TOLERANCE
            }
        if (!restored) {
            val metrics = realDisplayMetrics()
            failures +=
                AssertionError(
                    "뷰포트 복원이 확인되지 않았습니다. " +
                        "기대=${original.widthPx}x${original.heightPx}px@${original.densityDpi}dpi " +
                        "fontScale=${original.fontScale}, " +
                        "현재=${metrics.widthPixels}x${metrics.heightPixels}px@${metrics.densityDpi}dpi " +
                        "fontScale=${systemFontScale()}",
                )
        }

        val primary = failures.firstOrNull() ?: return
        failures.drop(1).forEach(primary::addSuppressed)
        throw primary
    }

    private fun awaitViewport(
        widthPx: Int,
        heightPx: Int,
        fontScale: Float,
    ) {
        val applied =
            awaitCondition {
                val metrics = realDisplayMetrics()
                metrics.widthPixels == widthPx &&
                    metrics.heightPixels == heightPx &&
                    metrics.densityDpi == VIEWPORT_DENSITY_DPI &&
                    abs(systemFontScale() - fontScale) < FONT_SCALE_TOLERANCE
            }
        // 앱 프로세스 설정까지 따라오면 기다린다. 다만 여기서 못 따라와도 실패로 보지 않는다.
        // Activity 는 기동 시점의 시스템 설정을 다시 받으므로, 실제 렌더 값 판정은 화면이 뜬 뒤
        // `assertRenderedViewport()` 가 한다.
        if (applied &&
            !awaitCondition(SOFT_TIMEOUT_MILLIS) { abs(applicationFontScale() - fontScale) < FONT_SCALE_TOLERANCE }
        ) {
            Log.w(TAG, "앱 프로세스 fontScale 이 ${applicationFontScale()} 로 남아 있습니다")
        }
        if (applied) return

        val metrics = realDisplayMetrics()
        throw AssertionError(
            "요청한 뷰포트가 반영되지 않았습니다. " +
                "요청=${widthPx}x${heightPx}px@${VIEWPORT_DENSITY_DPI}dpi fontScale=$fontScale, " +
                "현재=${metrics.widthPixels}x${metrics.heightPixels}px@${metrics.densityDpi}dpi " +
                "systemFontScale=${systemFontScale()} appFontScale=${applicationFontScale()}",
        )
    }

    /**
     * 설정 변경 콜백을 기다리며 조건을 다시 본다.
     *
     * 콜백이 오지 않는 경우에도 [POLL_SLICE_MILLIS] 마다 깨어나 다시 확인하므로, 콜백은 대기를
     * 짧게 끊어 주는 가속 신호일 뿐 판정 근거가 아니다.
     */
    private fun awaitCondition(
        timeoutMillis: Long = CONFIGURATION_TIMEOUT_MILLIS,
        matches: () -> Boolean,
    ): Boolean {
        if (matches()) return true
        val context = ApplicationProvider.getApplicationContext<Context>()
        val signals = LinkedBlockingQueue<Unit>()
        val callbacks =
            object : ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    signals.offer(Unit)
                }

                override fun onLowMemory() = Unit
            }
        context.registerComponentCallbacks(callbacks)
        try {
            val deadline = SystemClock.uptimeMillis() + timeoutMillis
            while (!matches()) {
                val remaining = deadline - SystemClock.uptimeMillis()
                if (remaining <= 0L) return false
                signals.poll(minOf(remaining, POLL_SLICE_MILLIS), TimeUnit.MILLISECONDS)
            }
            return true
        } finally {
            context.unregisterComponentCallbacks(callbacks)
        }
    }

    private fun systemFontScale(): Float {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        return Settings.System.getFloat(resolver, Settings.System.FONT_SCALE, 1f)
    }

    private fun applicationFontScale(): Float =
        ApplicationProvider
            .getApplicationContext<Context>()
            .resources.configuration.fontScale

    private fun overrideValue(
        output: String,
        prefix: String,
    ): String? =
        output
            .lineSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?.trim()

    private fun shell(command: String): String {
        val descriptor =
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).use { input -> input.readBytes().toString(Charsets.UTF_8) }
        }
    }

    /** 기기를 만지기 전 뷰포트. 복원 확인은 override 문자열이 아니라 여기 적힌 적용값으로 한다. */
    private class ViewportState(
        val sizeOverride: String?,
        val densityOverride: String?,
        val fontScaleSetting: String?,
        val widthPx: Int,
        val heightPx: Int,
        val densityDpi: Int,
        val fontScale: Float,
    )

    private companion object {
        const val TAG = "ViewportOverrideRule"
        const val CONFIGURATION_TIMEOUT_MILLIS = 20_000L

        /** 실패로 보지 않는 보조 대기. 앱 프로세스 설정 반영과 복원 확인에 쓴다. */
        const val SOFT_TIMEOUT_MILLIS = 5_000L
        const val POLL_SLICE_MILLIS = 100L
        const val FONT_SCALE_TOLERANCE = 0.01f
    }
}

/**
 * dp 를 px 로 바꾸는 배수. `wm density` 가 받는 dpi 는 이 값에 [DisplayMetrics.DENSITY_DEFAULT] 를
 * 곱한 [VIEWPORT_DENSITY_DPI] 다.
 */
private const val VIEWPORT_PX_PER_DP = 2

private const val VIEWPORT_DENSITY_DPI = DisplayMetrics.DENSITY_DEFAULT * VIEWPORT_PX_PER_DP

/** 시스템 데코를 포함한 실제 표시 크기. `wm size` 가 정한 값과 같은 좌표계다. */
private fun realDisplayMetrics(): DisplayMetrics {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val display =
        (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
            .getDisplay(Display.DEFAULT_DISPLAY)
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    display.getRealMetrics(metrics)
    return metrics
}

/** ancestor clip 이 hit 영역을 통째로 잘라, 어디를 눌러도 이 타깃에 닿지 않는 상태. */
private fun EnabledClickTarget.isClippedAway(): Boolean = width <= 0.dp || height <= 0.dp
