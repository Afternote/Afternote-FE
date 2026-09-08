package com.afternote.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.calendar.BottomSheetCalendar
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteTheme
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
        var type by mutableStateOf(PopupType.Default)

        composeRule.setContent {
            AfternoteTheme {
                Popup(
                    type = type,
                    message = "팝업",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText(confirm).assertExists()

        composeRule.runOnIdle { type = PopupType.Variant2 }

        composeRule.onNodeWithText(yes).assertExists()
        composeRule.onNodeWithText(no).assertExists()
    }

    @Test
    fun `달력 이동 버튼은 core 리소스 접근성 이름과 월 이동 동작을 유지한다`() {
        val previousMonth = stringResourceValue(R.string.core_ui_calendar_previous_month)
        val nextMonth = stringResourceValue(R.string.core_ui_calendar_next_month)

        composeRule.setContent {
            AfternoteTheme {
                BottomSheetCalendar(
                    onDismiss = {},
                    onDateSelect = {},
                    title = "날짜 선택",
                    initialDate = LocalDate.of(2026, 1, 15),
                )
            }
        }

        // 이동 결과는 콜백이 아니라 화면의 «년.월» 표기로 본다 — 월 상태는 달력이 스스로 들고 있다.
        composeRule.onNodeWithText(yearMonth(2026, 1)).assertExists()

        composeRule.onNodeWithContentDescription(previousMonth).performClick()
        composeRule.onNodeWithText(yearMonth(2025, 12)).assertExists()

        composeRule.onNodeWithContentDescription(nextMonth).performClick()
        composeRule.onNodeWithText(yearMonth(2026, 1)).assertExists()
    }

    private fun yearMonth(
        year: Int,
        month: Int,
    ): String = RuntimeEnvironment.getApplication().getString(R.string.core_ui_calendar_year_month, year, month)

    private fun stringResourceValue(resourceId: Int): String = RuntimeEnvironment.getApplication().getString(resourceId)
}
