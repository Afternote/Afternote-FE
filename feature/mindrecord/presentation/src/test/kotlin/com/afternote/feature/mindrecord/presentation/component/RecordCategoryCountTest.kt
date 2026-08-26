package com.afternote.feature.mindrecord.presentation.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.hometab.RecordCategoryCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.afternote.core.ui.R as CoreUiR

/**
 * 기록 카테고리 카드가 «모름» 을 0 으로 접지 않는지 (#700).
 *
 * 홈 ViewModel 이 enum 전체를 돌며 데일리질문·주간 리포트에 0 을 박아 넣고 있었다. 화면이
 * 일기 카드만 그려 안 보였을 뿐, 카드가 늘면 그 0 이 그대로 «기록 0건» 으로 노출된다.
 * 목업 숫자는 눈에 띄지만 0 은 그럴듯한 거짓이라 사용자가 그대로 믿는다 — 수신자 홈(#952)·
 * 주간 요약(#562)에서 같은 이유로 대시 표기를 택했다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordCategoryCountTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `기록 수를 모르면 0 이 아니라 대시를 그린다`() {
        composeRule.setContent {
            AfternoteTheme { card(totalCount = null) }
        }

        composeRule.onNodeWithText("–").assertIsDisplayed()
        composeRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun `0건은 확정값이라 그대로 그린다`() {
        composeRule.setContent {
            AfternoteTheme { card(totalCount = 0) }
        }

        composeRule.onNodeWithText("0").assertIsDisplayed()
    }

    @Test
    fun `실제 기록 수는 천 단위로 끊어 그린다`() {
        composeRule.setContent {
            AfternoteTheme { card(totalCount = 1234) }
        }

        composeRule.onNodeWithText("1,234").assertIsDisplayed()
    }

    @androidx.compose.runtime.Composable
    private fun card(totalCount: Int?) {
        RecordCategoryCard(
            iconResId = CoreUiR.drawable.core_ui_ic_diary,
            title = "일기",
            subtitle = "나의 매일을 기록하세요",
            totalCount = totalCount,
            onClick = {},
        )
    }
}
