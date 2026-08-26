package com.afternote.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.calendar.DatePickerContent
import com.afternote.core.ui.popup.PopupContent
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CoreUiLocalizedTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `텍스트 필드 접근성 이름은 core 리소스 문구를 노출한다`() {
        val searchDescription = stringResourceValue(R.string.core_ui_content_description_search)
        val backInputDescription =
            stringResourceValue(R.string.core_ui_content_description_resident_number_back_input)

        composeRule.setContent {
            AfternoteTheme {
                Column {
                    AfternoteTextField(
                        state = rememberTextFieldState(),
                        type = TextFieldType.Search,
                    )
                    AfternoteTextField(
                        state = rememberTextFieldState(),
                        type = TextFieldType.Variant8(backState = rememberTextFieldState()),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(searchDescription).assertExists()
        composeRule.onNodeWithContentDescription(backInputDescription).assertExists()
    }

    @Test
    fun `팝업 기본 버튼 문구는 타입별 core 리소스를 사용한다`() {
        val confirm = stringResourceValue(R.string.core_ui_popup_confirm)
        val yes = stringResourceValue(R.string.core_ui_popup_yes)
        val no = stringResourceValue(R.string.core_ui_popup_no)

        composeRule.setContent {
            AfternoteTheme {
                Column {
                    PopupContent(
                        type = PopupType.Default,
                        message = "기본 팝업",
                        onConfirm = {},
                        onDismiss = {},
                    )
                    PopupContent(
                        type = PopupType.Variant2,
                        message = "선택 팝업",
                        onConfirm = {},
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(confirm).assertExists()
        composeRule.onNodeWithText(yes).assertExists()
        composeRule.onNodeWithText(no).assertExists()
    }

    @Test
    fun `달력 이동 버튼은 core 리소스 접근성 이름과 클릭 동작을 유지한다`() {
        val previousMonth = stringResourceValue(R.string.core_ui_calendar_previous_month)
        val nextMonth = stringResourceValue(R.string.core_ui_calendar_next_month)
        var previousClicks = 0
        var nextClicks = 0

        composeRule.setContent {
            AfternoteTheme {
                DatePickerContent(
                    title = "날짜 선택",
                    currentYear = 2026,
                    currentMonth = 8,
                    selectedDate = LocalDate.of(2026, 8, 27),
                    onPrevMonth = { previousClicks += 1 },
                    onNextMonth = { nextClicks += 1 },
                    onDateSelect = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription(previousMonth).performClick()
        composeRule.onNodeWithContentDescription(nextMonth).performClick()

        composeRule.runOnIdle {
            assertEquals(1, previousClicks)
            assertEquals(1, nextClicks)
        }
    }

    private fun stringResourceValue(resourceId: Int): String = RuntimeEnvironment.getApplication().getString(resourceId)
}
