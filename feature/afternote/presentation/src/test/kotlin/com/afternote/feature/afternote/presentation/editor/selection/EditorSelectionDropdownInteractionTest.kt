package com.afternote.feature.afternote.presentation.editor.selection

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditorSelectionDropdownInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `비활성 드롭다운은 셰브론과 클릭 동작 및 펼친 메뉴를 노출하지 않는다`() {
        composeRule.setContent {
            AfternoteTheme {
                Box(modifier = Modifier.padding(24.dp)) {
                    EditorSelectionDropdown(
                        label = "종류",
                        selectedValue = "소셜 네트워크",
                        options = listOf("갤러리 및 파일"),
                        optionLabel = { it },
                        onValueSelected = {},
                        expanded = true,
                        onExpandedChange = {},
                        enabled = false,
                    )
                }
            }
        }

        composeRule.onNodeWithText("소셜 네트워크").assertExists()
        composeRule.onAllNodes(hasText("소셜 네트워크") and hasClickAction()).assertCountEquals(0)
        composeRule.onNodeWithContentDescription("드롭다운").assertDoesNotExist()
        composeRule.onNodeWithText("갤러리 및 파일").assertDoesNotExist()
    }
}
