package com.afternote.feature.home.presentation.receiver.component

import android.content.Context
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.home.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 「한 마디」 카드의 제목·작성일 배치 (#2032).
 *
 * 종전에는 제목과 작성일이 가중치 없이 한 `Row` 에 나란히 놓여, **긴 제목이 가용 폭을 다 쓰면
 * 뒤에 측정되는 작성일이 0dp 를 받았다.** 큰 글씨(배율 2.0)와 8글자 이름이 겹치는 조합에서
 * `2026.09.13` 이 열 줄로 한 글자씩 쪼개졌고, 카드 높이가 400dp 대까지 늘어 다음 섹션이 화면
 * 밖으로 밀렸다. 날짜 노드의 경계도 0 이라 접근성에서도 짚이지 않았다.
 *
 * 여기서 보는 것은 **폭 배분**이다 — 글자 크기·색이 아니라 「작성일이 자기 폭을 먼저 받고 한 줄로
 * 남는가」와 「제목도 폭을 잃지 않는가」다. 두 축을 함께 봐야 한쪽을 고치며 다른 쪽을 0 으로
 * 만드는 회귀가 잡힌다.
 *
 * 카드 폭 320dp 는 재현 조건 그대로다 — 360dp 화면에서 홈 좌우 여백 20dp 씩을 뺀 값이다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SenderMessageHeroCardLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `큰 글씨와 긴 이름에서도 작성일이 한 줄로 폭을 받는다`() {
        setCard(senderName = LONG_NAME, fontScale = 2f)

        val date = textLayoutOf(DATE)
        assertEquals("작성일이 한 글자씩 쪼개졌다", 1, date.lineCount)
        assertTrue("작성일이 0 너비를 받았다", date.size.width > 0)
    }

    @Test
    fun `큰 글씨와 긴 이름에서도 제목이 폭을 잃지 않는다`() {
        setCard(senderName = LONG_NAME, fontScale = 2f)

        val title = textLayoutOf(titleText(LONG_NAME))
        assertTrue("제목이 0 너비를 받았다", title.size.width > 0)
        // 한 글자씩 세로로 쪼개지지 않았는지 — 글자 수보다 줄이 적어야 한다.
        assertTrue("제목이 한 글자씩 쪼개졌다", title.lineCount < titleText(LONG_NAME).length)
    }

    /** 기본 배율·짧은 이름의 종전 표현을 지킨다 — 제목과 작성일이 한 줄씩이다. */
    @Test
    fun `기본 배율의 짧은 이름은 제목과 작성일이 각각 한 줄이다`() {
        setCard(senderName = SHORT_NAME, fontScale = 1f)

        assertEquals(1, textLayoutOf(titleText(SHORT_NAME)).lineCount)
        assertEquals(1, textLayoutOf(DATE).lineCount)
    }

    private fun setCard(
        senderName: String,
        fontScale: Float,
    ) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                AfternoteTheme {
                    SenderMessageHeroCard(
                        senderName = senderName,
                        date = DATE,
                        message = MESSAGE,
                        modifier = Modifier.width(CARD_WIDTH),
                    )
                }
            }
        }
    }

    private fun textLayoutOf(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(text)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single()
    }

    private fun titleText(senderName: String): String =
        ApplicationProvider
            .getApplicationContext<Context>()
            .getString(R.string.home_receiver_hero_title, senderName)

    private companion object {
        val CARD_WIDTH = 320.dp
        const val LONG_NAME = "김가나다라마바사"
        const val SHORT_NAME = "이준혁"
        const val DATE = "2026.09.13"
        const val MESSAGE = "잘 지내길 바랄게."
    }
}
