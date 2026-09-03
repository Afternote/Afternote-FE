package com.afternote.feature.mindrecord.presentation.hometab

import androidx.activity.ComponentActivity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MEMORIES 카드의 "그날의 기록 다시 읽기" 목적지 (#793).
 *
 * 처음에는 목적지가 미확정이라 인자를 비워 뒀고, 버튼이 자체 `clickable` 이라 클릭을 삼켜
 * «카드 여백을 누르면 추억 공간이 열리고, 정작 버튼만 아무 일도 안 한다» 였다.
 *
 * 디자인 게이트가 해제(2026-08-28)돼 목적지를 확정했다 — 문구가 「**그날의** 기록 다시 읽기」라
 * **카드가 보여 주는 그 한 건의 상세**로 간다. 카드·섹션 전체는 종전대로 추억 공간이다.
 *
 * 두 목적지가 갈린다는 것 자체가 계약이라, 섹션 클릭과 버튼 클릭을 따로 센다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MemoriesReadAgainTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `버튼은 섹션과 다른 목적지로 간다`() {
        var sectionClicks = 0
        var readAgainClicks = 0
        // MutableInteractionSource 는 @RememberInComposition 이라 컴포지션 안에서 맨손으로
        // 만들 수 없다 — 컴포지션마다 새 인스턴스가 생겨 상호작용 상태가 유실된다.
        val interactionSource = MutableInteractionSource()
        composeRule.setContent {
            AfternoteTheme {
                MemoriesSectionContent(
                    onMemoriesSectionClick = { sectionClicks += 1 },
                    onReadAgainClick = { readAgainClicks += 1 },
                    clickLabel = "추억 공간 열기",
                    interactionSource = interactionSource,
                    question = "가장 소중했던 순간은?",
                    answer = "아이가 태어났을 때.",
                )
            }
        }

        composeRule.onNodeWithText("그날의 기록 다시 읽기").performClick()

        // 버튼은 상세로만 간다 — 섹션 클릭까지 함께 세면 추억 공간이 겹쳐 열린다.
        assertEquals("버튼이 상세로 가지 않았다", 1, readAgainClicks)
        assertEquals("버튼 클릭이 섹션 클릭까지 태웠다", 0, sectionClicks)
    }

    @Test
    fun `카드 본문을 누르면 섹션 목적지로 간다`() {
        var sectionClicks = 0
        var readAgainClicks = 0
        val interactionSource = MutableInteractionSource()
        composeRule.setContent {
            AfternoteTheme {
                MemoriesSectionContent(
                    onMemoriesSectionClick = { sectionClicks += 1 },
                    onReadAgainClick = { readAgainClicks += 1 },
                    clickLabel = "추억 공간 열기",
                    interactionSource = interactionSource,
                    question = "가장 소중했던 순간은?",
                    answer = "아이가 태어났을 때.",
                )
            }
        }

        composeRule.onNodeWithText("가장 소중했던 순간은?").performClick()

        assertEquals("카드 본문이 섹션 목적지로 가지 않았다", 1, sectionClicks)
        assertEquals("카드 본문이 상세까지 태웠다", 0, readAgainClicks)
    }

    @Test
    fun `기록이 0건이면 다시 읽기 버튼을 그리지 않는다`() {
        // 문구가 「**그날의** 기록 다시 읽기」다 — 열 기록이 없는데 버튼을 남기면 그 약속을
        // 지킬 목적지가 없다. 다른 곳으로 보내는 대신 버튼을 안 그린다 (#793 리뷰).
        val interactionSource = MutableInteractionSource()
        composeRule.setContent {
            AfternoteTheme {
                MemoriesSectionContent(
                    onMemoriesSectionClick = {},
                    onReadAgainClick = null,
                    clickLabel = "추억 공간 열기",
                    interactionSource = interactionSource,
                )
            }
        }

        composeRule.onNodeWithText("그날의 기록 다시 읽기").assertDoesNotExist()
    }
}
