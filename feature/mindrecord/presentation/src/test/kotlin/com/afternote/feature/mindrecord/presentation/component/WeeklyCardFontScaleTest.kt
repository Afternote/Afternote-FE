package com.afternote.feature.mindrecord.presentation.component

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 주간 요약 카드가 **사용자 글자 크기를 따라 자라는지** (#1718).
 *
 * 종전에는 카드가 `height(200.dp)` 로 못 박혀 있었다. 안의 내용은 텍스트 다섯 줄이고 전부
 * 사용자 폰트 배율을 따르므로, 배율을 올리면 내용이 200dp 를 넘고 마지막 행인 카운트
 * 라벨(「데일리 질문」·「일기」)부터 카드 경계에 잘렸다. 실기 실측에서 배율 1.3 에 재현됐다.
 *
 * 글자 크기를 키우는 것은 접근성 설정의 기본 사용이라, **저시력 사용자가 정확히 이 화면에서
 * 값을 잃는다.**
 *
 * ### 「자란다」만 보지 않는다
 *
 * 높이를 그냥 `wrapContentHeight` 로 풀면 배율 1.0 에서 카드가 시안(200dp)보다 작아진다.
 * 그래서 **기본 배율의 높이가 그대로인 것**과 **배율을 올리면 잘리지 않는 것**을 함께 본다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class WeeklyCardFontScaleTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `기본 배율에서는 시안 높이를 그대로 쓴다`() {
        renderCard(fontScale = 1.0f)

        val card = composeRule.onRoot().getUnclippedBoundsInRoot()
        assertEquals(200.dp, card.bottom - card.top)
    }

    /**
     * 배율은 **2.0** 으로 잡는다. Android 접근성 설정의 최대치이고, Robolectric 폰트 메트릭이
     * 실기와 미세하게 달라 1.3 에서는 4dp 여유로 아슬하게 들어가 버린다(실측: 라벨 bottom
     * 176dp / 내용 영역 180dp). 실기에서는 1.3 에서 이미 잘렸다 — 여기서는 경계를 넘기는
     * 값으로 «자라는가» 를 확실히 가른다.
     */
    @Test
    fun `글자를 키우면 카드가 함께 자라 라벨이 잘리지 않는다`() {
        renderCard(fontScale = 2.0f)

        val card = composeRule.onRoot().getUnclippedBoundsInRoot()
        val label = composeRule.onNodeWithText(diaryLabel()).getUnclippedBoundsInRoot()

        assertTrue(
            "카드가 시안 높이에 묶여 자라지 않는다: ${card.bottom - card.top}",
            card.bottom - card.top > 200.dp,
        )
        assertTrue(
            "카운트 라벨이 카드 밖으로 밀려 잘린다: 라벨 bottom=${label.bottom}, 카드 bottom=${card.bottom}",
            label.bottom <= card.bottom,
        )
    }

    private fun diaryLabel(): String =
        composeRule.activity.getString(
            com.afternote.feature.mindrecord.presentation.R.string.mindrecord_category_diary_title,
        )

    private fun renderCard(fontScale: Float) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = base.density, fontScale = fontScale),
            ) {
                AfternoteTheme { WeeklyReportReviewCard(onWeekSelect = {}) }
            }
        }
    }
}
