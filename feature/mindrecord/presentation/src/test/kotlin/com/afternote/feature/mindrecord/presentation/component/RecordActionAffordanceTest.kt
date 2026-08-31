package com.afternote.feature.mindrecord.presentation.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * «더보기» 는 실제로 동작할 때만 그린다 (#1540).
 *
 * 종전에는 `onEdit`·`onDelete` 가 `= {}` 디폴트라, 핸들러를 넘기지 않은 화면에서도 메뉴가
 * 뜨고 «수정»·«삭제» 를 눌러도 아무 일이 없었다. 실제로 주간 리포트 HISTORY 가 그 상태였다
 * (`WeeklyReportScreen` 이 `DailyQuestionListCard(answer = …)` 만 넘기고 있었다).
 *
 * 디폴트를 걷어내 컴파일이 배선을 강제하게 했고, 상호작용이 없는 자리는 `null` 로 **버튼
 * 자체를 숨긴다.** 「눌러도 아무 일 없는 버튼」을 그리지 않는 것이 이 가드의 계약이다
 * (#582 · #618 · #722 · #777 전례).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordActionAffordanceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val answer =
        DailyQuestion(
            title = "오늘 하루, 누구에게 가장 고마웠나요?",
            content = "아무 말 없이 곁을 지켜주는 사람.",
            date = LocalDate.of(2026, 8, 30),
        )

    @Test
    fun `핸들러가 없으면 더보기를 그리지 않는다`() {
        composeRule.setContent {
            AfternoteTheme {
                DailyQuestionListCard(
                    answer = answer,
                    onEdit = null,
                    onDelete = null,
                    onClick = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(moreMenuLabel())
            .assertDoesNotExist()
    }

    @Test
    fun `핸들러가 있으면 더보기를 그린다`() {
        composeRule.setContent {
            AfternoteTheme {
                DailyQuestionListCard(
                    answer = answer,
                    onEdit = {},
                    onDelete = {},
                    onClick = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(moreMenuLabel())
            .assertIsDisplayed()
    }

    private fun moreMenuLabel(): String = composeRule.activity.getString(R.string.mindrecord_more_menu_cd)
}
