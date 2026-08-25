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
 * MEMORIES 카드의 "그날의 기록 다시 읽기" 가 죽은 영역이 아닌지 (#793).
 *
 * 목적지가 미확정이라 인자를 비워 뒀는데, 버튼이 자체 `clickable` 이라 클릭을 삼켰다.
 * 결과는 «카드 여백을 누르면 추억 공간이 열리고, 정작 버튼만 아무 일도 안 한다» 였다.
 * 최종 목적지가 확정될 때까지 버튼은 자기 컨테이너와 같은 곳으로 간다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MemoriesReadAgainTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `버튼을 누르면 카드와 같은 목적지로 간다`() {
        var clicks = 0
        composeRule.setContent {
            AfternoteTheme {
                MemoriesSectionContent(
                    onMemoriesSectionClick = { clicks += 1 },
                    clickLabel = "추억 공간 열기",
                    interactionSource = MutableInteractionSource(),
                    question = "가장 소중했던 순간은?",
                    answer = "아이가 태어났을 때.",
                )
            }
        }

        composeRule.onNodeWithText("그날의 기록 다시 읽기").performClick()

        assertEquals(1, clicks)
    }
}
