package com.afternote.feature.mindrecord.presentation.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.viewmodel.WeekOption
import com.afternote.feature.mindrecord.presentation.viewmodel.buildWeekOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * 열린 주차 메뉴의 스크롤·최하단 선택 회귀 가드 (#729).
 *
 * 시안(node 700-35071)의 열린 메뉴는 항목 5개 높이에서 잘리고 오른쪽에 세로 스크롤
 * 표시가 있다. 선택지가 5개에서 끝나면 스크롤할 것이 없어 이 시연 자체가 불가능하다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeeklyReportWeekMenuTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 8, 23)
    private val options: List<WeekOption> = buildWeekOptions(today = today)

    private fun label(monday: LocalDate): String = "${monday.monthValue}월 ${(monday.dayOfMonth - 1) / 7 + 1}주차 리포트"

    private fun openMenu(onWeekSelect: (LocalDate) -> Unit = {}) {
        composeRule.setContent {
            AfternoteTheme {
                WeeklyReportReviewCard(
                    selectedMonday = options.first().monday,
                    weekOptions = options,
                    onWeekSelect = onWeekSelect,
                )
            }
        }
        composeRule.onNodeWithText(label(options.first().monday)).performClick()
    }

    @Test
    fun `열린 메뉴에 5개를 넘는 선택지가 실린다`() {
        openMenu()

        // 6번째 항목은 메뉴 높이 밖이라 스크롤해야 닿는다 — 존재 자체는 트리에 있다.
        composeRule.onNodeWithText(label(options[5].monday)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `스크롤해서 최하단 주차를 고르면 그 월요일이 전달된다`() {
        var selected: LocalDate? = null
        openMenu { selected = it }

        val bottom = options.last().monday
        composeRule.onNodeWithText(label(bottom)).performScrollTo().performClick()

        assertEquals(bottom, selected)
        assertEquals(LocalDate.of(2026, 8, 17).minusWeeks(51), selected)
    }

    @Test
    fun `이미 보고 있는 주를 다시 고르면 재조회하지 않는다`() {
        var calls = 0
        openMenu { calls++ }

        // 같은 문구가 앵커와 메뉴 항목 두 곳에 있다 — 메뉴 쪽(마지막)을 누른다.
        val nodes = composeRule.onAllNodesWithText(label(options.first().monday))
        nodes[nodes.fetchSemanticsNodes().lastIndex].performClick()

        assertEquals(0, calls)
    }
}
