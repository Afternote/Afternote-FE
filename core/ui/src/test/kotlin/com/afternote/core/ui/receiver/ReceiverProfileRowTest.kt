package com.afternote.core.ui.receiver

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [ReceiverProfileRow] 공용 계약 — 선택 화면(체크박스)과 설정 관리 화면(화살표)이
 * 공유하는 이름·관계 렌더링과 행 전체 클릭만 여기서 고정한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReceiverProfileRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `이름과 관계를 표시한다`() {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverProfileRow(name = "김수신", relation = "가족", onClick = {})
            }
        }

        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onNodeWithText("가족").assertIsDisplayed()
    }

    @Test
    fun `이름 영역을 탭하면 onClick 이 호출된다`() {
        var clicks = 0
        composeRule.setContent {
            AfternoteTheme {
                ReceiverProfileRow(name = "김수신", relation = "가족", onClick = { clicks += 1 })
            }
        }

        composeRule.onNodeWithText("김수신").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `trailing 슬롯 내용을 그린다`() {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverProfileRow(
                    name = "김수신",
                    relation = "가족",
                    onClick = {},
                    trailing = { Text("트레일링") },
                )
            }
        }

        composeRule.onNodeWithText("트레일링").assertIsDisplayed()
    }
}
