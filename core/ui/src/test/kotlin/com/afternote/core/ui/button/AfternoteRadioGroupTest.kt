package com.afternote.core.ui.button

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteRadioGroupTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `nullable 미선택에서 누른 값 하나만 선택하고 정확한 값을 전달한다`() {
        val selectedValues = mutableListOf<String>()
        composeRule.setContent {
            var selectedValue by remember { mutableStateOf<String?>(null) }

            AfternoteTheme {
                AfternoteRadioGroup(
                    options = OPTIONS,
                    selectedValue = selectedValue,
                    onSelect = {
                        selectedValues += it
                        selectedValue = it
                    },
                ) { option, _ ->
                    Text(text = option)
                }
            }
        }

        composeRule.onAllNodes(radioRoleMatcher).apply {
            assertCountEquals(2)
            get(0).assertIsNotSelected()
            get(1).assertIsNotSelected()
            get(1).performClick()
            get(0).assertIsNotSelected()
            get(1).assertIsSelected()
            get(0).performClick()
            get(0).assertIsSelected()
            get(1).assertIsNotSelected()
        }

        composeRule.runOnIdle {
            assertEquals(listOf("두 번째", "첫 번째"), selectedValues)
        }
    }

    @Test
    fun `옵션마다 하나의 라디오 semantics와 48dp 터치 영역만 노출한다`() {
        composeRule.setContent {
            AfternoteTheme {
                AfternoteRadioGroup(
                    options = OPTIONS,
                    selectedValue = OPTIONS.first(),
                    onSelect = {},
                ) { option, _ ->
                    Text(text = option)
                }
            }
        }

        composeRule.onAllNodes(radioRoleMatcher).assertCountEquals(2)
        composeRule.onAllNodes(radioRoleMatcher, useUnmergedTree = true).apply {
            assertCountEquals(2)
            get(0).assertIsSelected()
            get(1).assertIsNotSelected()
            repeat(2) { index ->
                val bounds = get(index).getUnclippedBoundsInRoot()
                assertTrue(bounds.right - bounds.left >= 48.dp)
                assertTrue(bounds.bottom - bounds.top >= 48.dp)
            }
        }
        composeRule.onAllNodes(selectedMatcher).assertCountEquals(1)
        composeRule.onAllNodes(selectedMatcher, useUnmergedTree = true).assertCountEquals(1)
    }

    private companion object {
        val OPTIONS = listOf("첫 번째", "두 번째")
        val radioRoleMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        val selectedMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
    }
}
