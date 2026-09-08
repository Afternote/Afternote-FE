package com.afternote.feature.afternote.presentation.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.assertAccessibleClickTargets
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.editor.message.EditorMessageSection
import com.afternote.feature.afternote.presentation.editor.message.LeaveMessageEditorItem
import com.afternote.feature.afternote.presentation.editor.message.LeaveMessageEditorItemState
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodCheckbox
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiverList
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiverListState
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditorTouchTargetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `registered message toggle and add expose named button callbacks`() {
        var addClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                EditorMessageSection(
                    messages =
                        listOf(
                            LeaveMessageEditorItem(
                                titleState = TextFieldState("Registered"),
                                contentState = TextFieldState("Body"),
                                initialState = LeaveMessageEditorItemState.REGISTERED_COLLAPSED,
                            ),
                        ),
                    onRegisterClick = {},
                    onDeleteClick = {},
                    onAddClick = { addClicks++ },
                )
            }
        }

        composeRule.assertAccessibleClickTargets()
        val targets = composeRule.scanEnabledClickTargets()
        assertEquals(Role.Button, targets.single { it.name == "등록된 말씀 펼치기" }.role)
        assertEquals(Role.Button, targets.single { it.name == "남기실 말씀 추가" }.role)

        composeRule.onNodeWithContentDescription("등록된 말씀 펼치기").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("등록된 말씀 접기").assertExists()
        composeRule.onNodeWithContentDescription("남기실 말씀 추가").performClick()
        assertEquals(1, addClicks)
    }

    @Test
    fun `editing message delete and register targets invoke their callbacks`() {
        var registerClicks = 0
        var deleteClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                EditorMessageSection(
                    messages =
                        listOf(
                            LeaveMessageEditorItem(
                                titleState = TextFieldState("Editing"),
                                contentState = TextFieldState("Body"),
                            ),
                        ),
                    onRegisterClick = { registerClicks++ },
                    onDeleteClick = { deleteClicks++ },
                    onAddClick = {},
                )
            }
        }

        val targets = composeRule.scanEnabledClickTargets()
        val delete = targets.single { it.name == "삭제" }
        val register = targets.single { it.name == "등록" }
        assertEquals(Role.Button, delete.role)
        assertEquals(Role.Button, register.role)
        assertFalse(delete.isSmallerThan(MinimumTouchTargetSize))
        assertFalse(register.isSmallerThan(MinimumTouchTargetSize))

        composeRule.onNodeWithText("삭제").performClick()
        composeRule.onNodeWithText("등록").performClick()
        assertEquals(1, deleteClicks)
        assertEquals(1, registerClicks)
    }

    @Test
    fun `processing and receiver more actions are distinct named button callbacks`() {
        var processingMoreClicks = 0
        var receiverAddClicks = 0
        val deletedReceiverIds = mutableListOf<Long>()
        val receiverState = AfternoteEditorReceiverListState()
        composeRule.setContent {
            AfternoteTheme {
                Column {
                    ProcessingMethodCheckbox(
                        item = ProcessingMethodItem(1, "Method"),
                        onMoreClick = { processingMoreClicks++ },
                        onDismissDropdown = {},
                        onEditClick = {},
                        onDeleteClick = {},
                        onEditConfirmed = {},
                    )
                    AfternoteEditorReceiverList(
                        afternoteEditReceivers =
                            listOf(AfternoteEditorReceiver(id = 1L, name = "Name", label = "Family")),
                        onAddClick = { receiverAddClicks++ },
                        onItemDeleteClick = { deletedReceiverIds += it },
                        state = receiverState,
                    )
                }
            }
        }

        composeRule.assertAccessibleClickTargets()
        val moreTargets = composeRule.scanEnabledClickTargets().filter { it.name == "더보기" }
        assertEquals(2, moreTargets.size)
        assertEquals(listOf(Role.Button, Role.Button), moreTargets.map { it.role })

        val moreNodes = composeRule.onAllNodesWithContentDescription("더보기")
        moreNodes[0].performClick()
        moreNodes[1].performClick()
        composeRule.waitForIdle()
        assertEquals(1, processingMoreClicks)
        assertEquals(true, receiverState.expandedStates[1L])

        composeRule.onNodeWithText("삭제하기").performClick()
        composeRule.onNodeWithContentDescription("추가").performClick()
        assertEquals(listOf(1L), deletedReceiverIds)
        assertEquals(1, receiverAddClicks)
    }

    @Test
    fun `editor top back and submit expose named button callbacks`() {
        var backClicks = 0
        var registerClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                AfternoteEditorScreen(
                    form = EditorFormState(),
                    onBackClick = { backClicks++ },
                    onRegisterClick = { registerClicks++ },
                    snackbarMessage = null,
                    onSnackbarMessageConsumed = {},
                    validationMessage = null,
                    onValidationMessageConsumed = {},
                    content = {},
                )
            }
        }

        composeRule.assertAccessibleClickTargets()
        val targets = composeRule.scanEnabledClickTargets()
        assertEquals(Role.Button, targets.single { it.name == "뒤로가기" }.role)
        assertEquals(Role.Button, targets.single { it.name == "등록" }.role)

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeRule.onNodeWithText("등록").performClick()
        assertEquals(1, backClicks)
        assertEquals(1, registerClicks)
    }
}
