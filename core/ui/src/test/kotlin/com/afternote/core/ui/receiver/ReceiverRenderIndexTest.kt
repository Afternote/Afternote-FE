package com.afternote.core.ui.receiver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 초성 인덱스 점프 위치가 실제로 그려지는 행 순서와 같은지 고정한다 (#939).
 *
 * 점프 인덱스를 그룹 길이 합산으로 따로 계산하면 렌더 구조와 갈라져 그룹이 하나 지날 때마다
 * 어긋나는데, 그룹이 둘뿐이면 첫 그룹만 탭해 봐야 오차가 0 이라 안 잡힌다. 그래서 여기서는
 * **그룹 3개 이상 · 그룹마다 2명 이상**으로 깔고 두 번째·세 번째 그룹을 탭한다.
 *
 * 기대 인덱스(헤더를 그리지 않으므로 행 수만 누적):
 *
 * | 초성 | 행 | 인덱스 |
 * |---|---|---|
 * | ㄱ | 가수신1 · 가수신2 · 가제외 | 0 |
 * | ㄴ | 나수신1 · 나수신2 | 3 |
 * | ㄷ | 다수신1 · 다수신2 | 5 |
 * | ㅎ | 하수신01 ~ 하수신20 | 7 |
 *
 * 헤더를 세는 옛 계산(`index += 1 + items.size`)이면 ㄴ 은 4, ㄷ 은 7 로 밀려 그룹 첫 행을
 * 지나친다. 꼬리의 ㅎ 20행은 목록 끝 클램프가 그 어긋남을 가려 주지 못하게 두는 채움이다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class ReceiverRenderIndexTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `두 번째 그룹 초성을 탭하면 그 그룹 첫 행이 목록 맨 위에 온다`() {
        setContent()

        tapConsonant("ㄴ")

        assertAtListTop(name = "나수신1", predecessor = "가제외")
    }

    @Test
    fun `세 번째 그룹 초성을 탭하면 그 그룹 첫 행이 목록 맨 위에 온다`() {
        setContent()

        tapConsonant("ㄷ")

        assertAtListTop(name = "다수신1", predecessor = "나수신2")
    }

    @Test
    fun `검색이 앞 그룹을 줄여 인덱스가 당겨져도 탭한 그룹 첫 행이 맨 위에 온다`() {
        setContent()

        // ㄱ 이 3행에서 2행으로 줄어 ㄴ 은 3 → 2, ㄷ 은 5 → 4 로 당겨진다.
        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("수신")

        tapConsonant("ㄷ")

        assertAtListTop(name = "다수신1", predecessor = "나수신2")
    }

    @Test
    fun `목록 자체가 교체돼 인덱스가 바뀌어도 탭한 그룹 첫 행이 맨 위에 온다`() {
        setContent()
        tapConsonant("ㄷ")

        // ㄱ 의 첫 행이 빠져 ㄴ 은 3 → 2, ㄷ 은 5 → 4 가 된다.
        composeRule.runOnIdle { displayedReceivers = receivers.filterNot { it.name == "가수신1" } }

        tapConsonant("ㄴ")

        assertAtListTop(name = "나수신1", predecessor = "가제외")
    }

    /**
     * 초성 [name] 그룹의 첫 행이 목록 뷰포트 맨 위에 통째로 보이는지 단언한다.
     *
     * 세 가지를 함께 본다 — 행이 보이고, 위 끝이 목록 위 끝과 같고, 바로 앞 행([predecessor])은
     * 화면 위로 밀려나 아예 없다. 인덱스가 밀리면 그 행은 뷰포트 밖이라 조회 자체가 실패한다.
     */
    private fun assertAtListTop(
        name: String,
        predecessor: String,
    ) {
        val listBounds =
            composeRule
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
                .getUnclippedBoundsInRoot()
        val rowBounds =
            composeRule
                .onNodeWithText(name)
                .assertIsDisplayed()
                .getUnclippedBoundsInRoot()

        assertEquals(listBounds.top.value, rowBounds.top.value, BOUNDS_TOLERANCE_DP)
        assertTrue(
            "$name 행이 목록 아래로 잘렸다: row=${rowBounds.bottom}, list=${listBounds.bottom}",
            rowBounds.bottom.value <= listBounds.bottom.value + BOUNDS_TOLERANCE_DP,
        )
        composeRule.onNodeWithText(predecessor).assertDoesNotExist()
    }

    private fun tapConsonant(consonant: String) {
        composeRule.onNodeWithText(consonant).performTouchInput { click() }
        composeRule.waitForIdle()
    }

    private var displayedReceivers by mutableStateOf(receivers)

    private fun setContent() {
        displayedReceivers = receivers
        composeRule.setContent {
            AfternoteTheme {
                ReceiverSelectScreen(
                    receivers = displayedReceivers,
                    selectedReceiverId = null,
                    onReceiverToggle = {},
                    onBackClick = {},
                    onConfirmClick = {},
                )
            }
        }
    }

    private companion object {
        const val BOUNDS_TOLERANCE_DP = 0.5f

        /**
         * ㄱ 3명 · ㄴ 2명 · ㄷ 2명 + 꼬리 ㅎ 20명.
         *
         * `가제외` 만 «수신» 검색에서 빠지도록 이름을 지어, 검색이 뒤 그룹 인덱스를 한 칸 당기게 한다.
         */
        val receivers =
            listOf(
                ReceiverSelectItem(id = 1L, name = "가수신1", relation = "가족"),
                ReceiverSelectItem(id = 2L, name = "가수신2", relation = "가족"),
                ReceiverSelectItem(id = 3L, name = "가제외", relation = "친구"),
                ReceiverSelectItem(id = 4L, name = "나수신1", relation = "친구"),
                ReceiverSelectItem(id = 5L, name = "나수신2", relation = "친구"),
                ReceiverSelectItem(id = 6L, name = "다수신1", relation = "동료"),
                ReceiverSelectItem(id = 7L, name = "다수신2", relation = "동료"),
            ) +
                List(20) { index ->
                    ReceiverSelectItem(
                        id = 100L + index,
                        name = "하수신%02d".format(index + 1),
                        relation = "친구",
                    )
                }
    }
}
