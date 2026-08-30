package com.afternote.core.ui.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.R
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.VIEW_MODE_INDICATOR_TEST_TAG
import com.afternote.core.ui.VIEW_MODE_PILL_TEST_TAG
import com.afternote.core.ui.ViewModeSwitcher
import com.afternote.core.ui.badge.CircularCheckboxOutlineChip
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.calendar.DatePickerContent
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.HomeTopBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class CoreTouchTargetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `small core actions keep visual size but expose named button targets`() {
        var settingClicks = 0
        var suffixClicks = 0
        var plusClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                Column {
                    HomeTopBar(onSettingClick = { settingClicks++ })
                    AfternoteTextField(
                        state = rememberTextFieldState(),
                        placeholder = "Field",
                        type = TextFieldType.Variant7(text = "Action", onClick = { suffixClicks++ }),
                    )
                    PlusBadgeButton(contentDescription = "Add", onClick = { plusClicks++ })
                }
            }
        }

        composeRule.assertAccessibleClickTargets()
        val targets = composeRule.scanEnabledClickTargets()
        val settingTarget = targets.single { it.name == "설정" }
        val plusTarget = targets.single { it.name == "Add" }
        assertEquals(Role.Button, settingTarget.role)
        assertEquals(Role.Button, targets.single { it.name == "Action" }.role)
        assertEquals(Role.Button, plusTarget.role)
        assertEquals(18f, settingTarget.layoutWidth.value, 0.1f)
        assertEquals(18f, settingTarget.layoutHeight.value, 0.1f)
        assertEquals(30f, plusTarget.layoutWidth.value, 0.1f)
        assertEquals(30f, plusTarget.layoutHeight.value, 0.1f)

        composeRule.onNodeWithContentDescription("설정", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Action", useUnmergedTree = true).performClick()
        composeRule.onNodeWithContentDescription("Add", useUnmergedTree = true).performClick()
        assertEquals(1, settingClicks)
        assertEquals(1, suffixClicks)
        assertEquals(1, plusClicks)
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp-xhdpi")
    fun `switcher chip and 320dp calendar expose 48dp named role state targets`() {
        val viewChanges = mutableListOf<Boolean>()
        var chipClicks = 0
        var previousClicks = 0
        var nextClicks = 0
        val selectedDays = mutableListOf<Int>()
        composeRule.setContent {
            AfternoteTheme {
                Column {
                    ViewModeSwitcher(
                        isListView = true,
                        onViewChange = viewChanges::add,
                        image1 = R.drawable.core_ui_list,
                        image2 = R.drawable.core_ui_calendar,
                    )
                    CircularCheckboxOutlineChip(label = "Receiver", onClick = { chipClicks++ })
                    DatePickerContent(
                        title = "Date",
                        currentYear = 2026,
                        currentMonth = 8,
                        selectedDate = LocalDate.of(2026, 8, 28),
                        onPrevMonth = { previousClicks++ },
                        onNextMonth = { nextClicks++ },
                        onDateSelect = selectedDays::add,
                    )
                }
            }
        }

        composeRule.assertAccessibleClickTargets()
        val targets = composeRule.scanEnabledClickTargets()
        val listTarget = targets.single { it.name == "목록 보기" }
        val calendarTarget = targets.single { it.name == "달력 보기" }
        val selectedDay = targets.single { it.name == "28" }
        val chipTarget = targets.single { it.name == "Receiver" }
        val previousTarget = targets.single { it.name == "이전 달" }
        val nextTarget = targets.single { it.name == "다음 달" }
        assertEquals(Role.RadioButton, listTarget.role)
        assertEquals(true, listTarget.selected)
        assertEquals(Role.RadioButton, calendarTarget.role)
        assertEquals(false, calendarTarget.selected)
        assertEquals(Role.RadioButton, selectedDay.role)
        assertEquals(true, selectedDay.selected)
        assertEquals(Role.Button, chipTarget.role)
        assertEquals(Role.Button, previousTarget.role)
        assertEquals(Role.Button, nextTarget.role)
        assertFalse(targets.any { it.isSmallerThan(MinimumTouchTargetSize) })

        composeRule.onNodeWithContentDescription("목록 보기", useUnmergedTree = true).performClick()
        composeRule.onNodeWithContentDescription("달력 보기", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Receiver").performClick()
        composeRule.onNodeWithContentDescription("이전 달").performClick()
        composeRule.onNodeWithContentDescription("다음 달").performClick()
        composeRule.onNodeWithText("28").performClick()
        assertEquals(listOf(true, false), viewChanges)
        assertEquals(1, chipClicks)
        assertEquals(1, previousClicks)
        assertEquals(1, nextClicks)
        assertEquals(listOf(28), selectedDays)

        composeRule.assertNodeSize(VIEW_MODE_PILL_TEST_TAG, width = 68f, height = 36f)
        composeRule.assertNodeSize(VIEW_MODE_INDICATOR_TEST_TAG, width = 28f, height = 28f)
        assertTrue(selectedDay.width >= 48.dp && selectedDay.height >= 48.dp)
    }

    private fun ComposeContentTestRule.assertNodeSize(
        tag: String,
        width: Float,
        height: Float,
    ) {
        val bounds = onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(width, (bounds.right - bounds.left).value, 0.1f)
        assertEquals(height, (bounds.bottom - bounds.top).value, 0.1f)
    }
}
