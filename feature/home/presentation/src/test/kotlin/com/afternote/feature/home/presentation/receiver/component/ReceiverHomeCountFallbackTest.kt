package com.afternote.feature.home.presentation.receiver.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.home.presentation.receiver.model.MindRecordSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 기록 수 조회 실패 표기 (#952, 시안 4309:19394).
 *
 * 확정값은 «개수 자리만 대시, 레이아웃은 유지» 다. 종전에는 문장을 통째로 "기록 수를
 * 불러오지 못했습니다." 로 갈아 끼우고 통계 카드 행을 숨겨, 실패한 화면과 정상 화면의
 * 구성이 달랐다 — 카드가 통째로 사라지면 «기능이 없는 것» 과 «못 불러온 것» 도
 * 구분되지 않는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverHomeCountFallbackTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `조회에 실패해도 문장 구조가 그대로다`() {
        composeRule.setContent {
            AfternoteTheme { MindRecordSection(summary = null, onGoClick = {}) }
        }

        // 종전 문구가 아니라, 개수 자리만 바뀐 같은 문장이어야 한다.
        composeRule.onNodeWithText("–개 마음의 기록이 있습니다.").assertIsDisplayed()
    }

    @Test
    fun `조회에 실패해도 통계 카드가 남는다`() {
        composeRule.setContent {
            AfternoteTheme { MindRecordSection(summary = null, onGoClick = {}) }
        }

        composeRule.onNodeWithText("데일리 질문").assertIsDisplayed()
        composeRule.onNodeWithText("일기").assertIsDisplayed()
        // 두 카드의 개수 자리가 «모두» 대시다 — 인덱스 하나만 보면 한 장만 대시로 남는
        // 회귀를 놓치고, 노드 순서가 바뀌면 조용히 다른 것을 본다.
        composeRule.onAllNodesWithText("–").assertCountEquals(2)
    }

    @Test
    fun `정상 조회는 숫자를 그대로 보여준다`() {
        composeRule.setContent {
            AfternoteTheme {
                MindRecordSection(
                    summary = MindRecordSummary(dailyQuestionCount = 18, diaryCount = 4),
                    onGoClick = {},
                )
            }
        }

        composeRule.onNodeWithText("22개 마음의 기록이 있습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("18").assertIsDisplayed()
        composeRule.onNodeWithText("4").assertIsDisplayed()
    }
}
