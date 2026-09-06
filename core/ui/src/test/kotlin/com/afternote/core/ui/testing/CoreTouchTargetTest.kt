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
import com.afternote.core.ui.ViewModeSwitcher
import com.afternote.core.ui.badge.CircularCheckboxOutlineChip
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.calendar.BottomSheetCalendar
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
    fun `switcher and chip expose 48dp named role state targets`() {
        val viewChanges = mutableListOf<Boolean>()
        var chipClicks = 0
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
                }
            }
        }

        composeRule.assertAccessibleClickTargets()
        val targets = composeRule.scanEnabledClickTargets()
        val listTarget = targets.single { it.name == "목록 보기" }
        val calendarTarget = targets.single { it.name == "달력 보기" }
        val chipTarget = targets.single { it.name == "Receiver" }
        assertEquals(Role.RadioButton, listTarget.role)
        assertEquals(true, listTarget.selected)
        assertEquals(Role.RadioButton, calendarTarget.role)
        assertEquals(false, calendarTarget.selected)
        assertEquals(Role.Button, chipTarget.role)
        assertFalse(targets.any { it.isSmallerThan(MinimumTouchTargetSize) })

        composeRule.onNodeWithContentDescription("목록 보기", useUnmergedTree = true).performClick()
        composeRule.onNodeWithContentDescription("달력 보기", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Receiver").performClick()
        assertEquals(listOf(true, false), viewChanges)
        assertEquals(1, chipClicks)

        composeRule.assertNodeSize(VIEW_MODE_PILL_TAG, width = 68f, height = 36f)
        composeRule.assertNodeSize(VIEW_MODE_INDICATOR_TAG, width = 28f, height = 28f)
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp-xhdpi")
    fun `320dp calendar exposes 48dp named role state targets`() {
        val selectedDays = mutableListOf<LocalDate>()
        composeRule.setContent {
            AfternoteTheme {
                BottomSheetCalendar(
                    onDismiss = {},
                    onDateSelect = selectedDays::add,
                    title = "Date",
                    initialDate = LocalDate.of(2026, 8, 28),
                )
            }
        }

        val targets = composeRule.scanEnabledClickTargets()
        val selectedDay = targets.single { it.name == "28" }
        val previousTarget = targets.single { it.name == "이전 달" }
        val nextTarget = targets.single { it.name == "다음 달" }
        assertEquals(Role.RadioButton, selectedDay.role)
        assertEquals(true, selectedDay.selected)
        assertEquals(Role.Button, previousTarget.role)
        assertEquals(Role.Button, nextTarget.role)
        // 달력이 기여하는 타깃만 본다 — 시트 자체의 scrim·핸들은 Material3 소유라 이 가드 대상이 아니다.
        assertFalse(
            listOf(selectedDay, previousTarget, nextTarget).any { it.isSmallerThan(MinimumTouchTargetSize) },
        )

        composeRule.onNodeWithText("28").performClick()
        assertEquals(listOf(LocalDate.of(2026, 8, 28)), selectedDays)
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

    private companion object {
        /**
         * `ViewModeSwitcher` 의 태그 값 사본.
         *
         * 태그는 프로덕션 API 가 아니다 — 값을 공유하려고 선언의 visibility 를 넓히지 않고
         * 여기에 리터럴로 둔다 (#1672). 알약·인디케이터는 시각 전용이라 semantics 이름이 없어
         * 크기 회귀를 잡을 다른 앵커가 없다.
         */
        const val VIEW_MODE_PILL_TAG = "view_mode_pill"
        const val VIEW_MODE_INDICATOR_TAG = "view_mode_indicator"
    }
}
