package com.afternote.feature.setting.presentation.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
class RadioGroupTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `기존 index 계약으로 한 항목만 선택하고 선택 index를 전달한다`() {
        val selectedIndices = mutableListOf<Int>()
        composeRule.setContent {
            var selectedIndex by remember { mutableIntStateOf(0) }

            AfternoteTheme {
                RadioGroup(
                    items = ITEMS,
                    selectedIndex = selectedIndex,
                    onSelectIndex = {
                        selectedIndices += it
                        selectedIndex = it
                    },
                )
            }
        }

        composeRule.onAllNodes(radioRoleMatcher).apply {
            assertCountEquals(2)
            get(0).assertIsSelected()
            get(1).assertIsNotSelected()
            get(1).performClick()
            get(0).assertIsNotSelected()
            get(1).assertIsSelected()
            get(0).performClick()
            get(0).assertIsSelected()
            get(1).assertIsNotSelected()
        }
        composeRule.runOnIdle {
            assertEquals(listOf(1, 0), selectedIndices)
        }

        composeRule.onAllNodes(groupMatcher).assertCountEquals(1)
        composeRule.onAllNodes(radioRoleMatcher, useUnmergedTree = true).apply {
            assertCountEquals(2)
            repeat(2) { index ->
                val bounds = get(index).getUnclippedBoundsInRoot()
                assertTrue(bounds.right - bounds.left >= 48.dp)
                assertTrue(bounds.bottom - bounds.top >= 48.dp)
            }
        }
    }

    @Test
    fun `표시 내용이 같아도 index로 각 항목을 구분한다`() {
        val selectedIndices = mutableListOf<Int>()
        composeRule.setContent {
            var selectedIndex by remember { mutableIntStateOf(0) }
            AfternoteTheme {
                RadioGroup(
                    items = listOf(ITEMS.first(), ITEMS.first()),
                    selectedIndex = selectedIndex,
                    onSelectIndex = {
                        selectedIndices += it
                        selectedIndex = it
                    },
                )
            }
        }

        composeRule.onAllNodes(radioRoleMatcher).apply {
            assertCountEquals(2)
            get(0).assertIsSelected()
            get(1).assertIsNotSelected()
            get(1).performClick()
            get(0).assertIsNotSelected()
            get(1).assertIsSelected()
        }
        composeRule.runOnIdle {
            assertEquals(listOf(1), selectedIndices)
        }
    }

    @Test
    fun `범위 밖 index는 미선택으로 표시하고 선택한 index를 전달한다`() {
        val selectedIndex = mutableIntStateOf(-1)
        val selectedIndices = mutableListOf<Int>()
        composeRule.setContent {
            AfternoteTheme {
                RadioGroup(
                    items = ITEMS,
                    selectedIndex = selectedIndex.intValue,
                    onSelectIndex = {
                        selectedIndices += it
                        selectedIndex.intValue = it
                    },
                )
            }
        }

        composeRule.onAllNodes(selectedMatcher).assertCountEquals(0)
        composeRule.runOnIdle { selectedIndex.intValue = ITEMS.size }
        composeRule.onAllNodes(selectedMatcher).assertCountEquals(0)
        composeRule.onAllNodes(radioRoleMatcher)[1].performClick()
        composeRule.onAllNodes(radioRoleMatcher)[1].assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(listOf(1), selectedIndices)
        }
    }

    @Test
    fun `항목이 비면 선택 행을 제거하고 선택 callback을 호출하지 않는다`() {
        val items = mutableStateOf(ITEMS)
        val selectedIndices = mutableListOf<Int>()
        composeRule.setContent {
            AfternoteTheme {
                RadioGroup(
                    items = items.value,
                    selectedIndex = 0,
                    onSelectIndex = { selectedIndices += it },
                )
            }
        }

        composeRule.onAllNodes(radioRoleMatcher).assertCountEquals(2)
        composeRule.runOnIdle { items.value = emptyList() }
        composeRule.onAllNodes(radioRoleMatcher).assertCountEquals(0)
        composeRule.onAllNodes(groupMatcher).assertCountEquals(1)
        composeRule.runOnIdle {
            assertTrue(selectedIndices.isEmpty())
        }
    }

    private companion object {
        val ITEMS =
            listOf(
                RadioGroupItem(title = "첫 번째", description = "첫 번째 설명"),
                RadioGroupItem(title = "두 번째", description = "두 번째 설명"),
            )
        val radioRoleMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        val selectedMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
        val groupMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
    }
}
