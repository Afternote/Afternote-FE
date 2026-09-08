package com.afternote.core.ui.popup

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.R
import com.afternote.core.ui.testing.EnabledClickTarget
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [AfternoteActionMenu] 계약 가드 (#643).
 *
 * afternote·mindrecord·timeletter 4벌을 이 정본으로 모으므로, 자체 구현이 갖고 있던 계약
 * (선언 순서대로 렌더 · 클릭 전달 · 클릭 시 자동 dismiss · 48dp 터치 타깃)이 수렴 과정에서
 * 조용히 빠지지 않도록 여기서 고정한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class AfternoteActionMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `항목은 선언 순서대로 쌓이고 클릭은 그 항목에만 전달된다`() {
        val clicks = mutableListOf<String>()
        composeRule.setContent {
            AfternoteTheme {
                AfternoteActionMenu(
                    expanded = true,
                    onDismissRequest = {},
                    items =
                        listOf(
                            ActionMenuItem("첫째") { clicks += "첫째" },
                            ActionMenuItem("둘째") { clicks += "둘째" },
                            ActionMenuItem("셋째") { clicks += "셋째" },
                        ),
                )
            }
        }

        val first = composeRule.onNodeWithText("첫째").getUnclippedBoundsInRoot()
        val second = composeRule.onNodeWithText("둘째").getUnclippedBoundsInRoot()
        val third = composeRule.onNodeWithText("셋째").getUnclippedBoundsInRoot()
        assertTrue("첫째 는 둘째 위에 있어야 한다", first.top < second.top)
        assertTrue("둘째 는 셋째 위에 있어야 한다", second.top < third.top)

        composeRule.onNodeWithText("둘째").performClick()
        composeRule.runOnIdle { assertEquals(listOf("둘째"), clicks) }
    }

    @Test
    fun `항목 클릭은 dismiss 를 먼저 부르고 그다음 항목 콜백을 부른다`() {
        // 항목 콜백이 삭제 다이얼로그를 띄우는 호출부에서 메뉴가 뒤에 남지 않게 하는 계약이다.
        val events = mutableListOf<String>()
        composeRule.setContent {
            AfternoteTheme {
                AfternoteActionMenu(
                    expanded = true,
                    onDismissRequest = { events += "dismiss" },
                    items = listOf(ActionMenuItem("삭제하기") { events += "click" }),
                )
            }
        }

        composeRule.onNodeWithText("삭제하기").performClick()
        composeRule.runOnIdle { assertEquals(listOf("dismiss", "click"), events) }
    }

    @Test
    fun `expanded 가 false 면 아무 항목도 그리지 않는다`() {
        composeRule.setContent {
            AfternoteTheme {
                AfternoteActionMenu(
                    expanded = false,
                    onDismissRequest = {},
                    items = listOf(ActionMenuItem("삭제하기") {}),
                )
            }
        }

        composeRule.onNodeWithText("삭제하기").assertDoesNotExist()
    }

    @Test
    fun `수정 핸들러가 null 이면 수정 항목 자체가 빠진다`() {
        val editLabel = stringResourceValue(R.string.core_ui_action_menu_edit)
        val deleteLabel = stringResourceValue(R.string.core_ui_action_menu_delete)
        var deleted = 0
        composeRule.setContent {
            AfternoteTheme {
                AfternoteActionMenu(
                    expanded = true,
                    onDismissRequest = {},
                    items =
                        editDeleteActionMenuItems(
                            onEditClick = null,
                            onDeleteClick = { deleted++ },
                        ),
                )
            }
        }

        composeRule.onNodeWithText(editLabel).assertDoesNotExist()
        composeRule.onNodeWithText(deleteLabel).performClick()
        composeRule.runOnIdle { assertEquals(1, deleted) }
    }

    @Test
    fun `표준 쌍은 수정하기 다음 삭제하기 순서로 그려진다`() {
        val editLabel = stringResourceValue(R.string.core_ui_action_menu_edit)
        val deleteLabel = stringResourceValue(R.string.core_ui_action_menu_delete)
        composeRule.setContent {
            AfternoteTheme {
                AfternoteActionMenu(
                    expanded = true,
                    onDismissRequest = {},
                    items =
                        editDeleteActionMenuItems(
                            onEditClick = {},
                            onDeleteClick = {},
                        ),
                )
            }
        }

        val edit = composeRule.onNodeWithText(editLabel).getUnclippedBoundsInRoot()
        val delete = composeRule.onNodeWithText(deleteLabel).getUnclippedBoundsInRoot()
        assertTrue("«수정하기» 가 «삭제하기» 위에 있어야 한다", edit.top < delete.top)
    }

    @Test
    fun `항목은 48dp 터치 타깃과 버튼 역할을 갖춘다`() {
        composeRule.setContent {
            AfternoteTheme {
                AfternoteActionMenu(
                    expanded = true,
                    onDismissRequest = {},
                    items = editDeleteActionMenuItems(onEditClick = {}, onDeleteClick = {}),
                )
            }
        }

        val targets = composeRule.scanEnabledClickTargets()
        assertEquals(2, targets.size)
        targets.forEach { target ->
            assertEquals(Role.Button, target.role)
            assertTrue(target.diagnosticName(), !target.isSmallerThan(MinimumTouchTargetSize))
        }
    }

    @Test
    fun `빈 목록은 호출부 오류로 터뜨린다`() {
        // 항목을 숨기는 건 리스트에서 빼는 것으로 표현하지만, 전부 빼고 메뉴를 여는 호출은 버그다.
        assertThrows(IllegalArgumentException::class.java) {
            composeRule.setContent {
                AfternoteTheme {
                    AfternoteActionMenu(
                        expanded = false,
                        onDismissRequest = {},
                        items = emptyList(),
                    )
                }
            }
        }
    }

    private fun EnabledClickTarget.diagnosticName(): String = "«$name» 이 최소 터치 타깃보다 작다: $width×$height"

    private fun stringResourceValue(resourceId: Int): String = RuntimeEnvironment.getApplication().getString(resourceId)
}
