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
        var saveDraftClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                AfternoteEditorScreen(
                    form = EditorFormState(),
                    onBackClick = { backClicks++ },
                    onSaveDraftClick = { saveDraftClicks++ },
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
        assertEquals(Role.Button, targets.single { it.name == "임시저장" }.role)

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeRule.onNodeWithText("등록").performClick()
        composeRule.onNodeWithText("임시저장").performClick()
        assertEquals(1, backClicks)
        assertEquals(1, registerClicks)
        assertEquals(1, saveDraftClicks)
    }

    /**
     * 「임시저장」은 「등록」과 같은 게이트를 받는다 (#808 리뷰).
     *
     * 저장이 나가 있는 동안·prefill 을 못 읽은 동안 이 버튼만 살아 있으면, 누른 탭이 VM 의 `isSaving`
     * 가드에 걸려 오류도 스낵바도 없이 삼켜진다. 게이트가 내려간 동안은 클릭 타깃에서 빠지고 콜백도
     * 불리지 않아야 한다 — `enabled` 를 지우면 아래 두 단언이 함께 빨개진다.
     */
    @Test
    fun `editor top bar disables the draft action together with submit`() {
        var saveDraftClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                AfternoteEditorScreen(
                    form = EditorFormState(),
                    onBackClick = {},
                    onSaveDraftClick = { saveDraftClicks++ },
                    onRegisterClick = {},
                    isSubmitEnabled = false,
                    snackbarMessage = null,
                    onSnackbarMessageConsumed = {},
                    validationMessage = null,
                    onValidationMessageConsumed = {},
                    content = {},
                )
            }
        }

        composeRule.assertAccessibleClickTargets()
        val enabledNames = composeRule.scanEnabledClickTargets().map { it.name }
        assertEquals(emptyList<String>(), enabledNames.filter { it == "임시저장" || it == "등록" })

        composeRule.onNodeWithText("임시저장").performClick()
        assertEquals(0, saveDraftClicks)
    }

    /**
     * 콜백이 없으면 버튼 자체가 없어야 한다 (#808).
     *
     * 저장한 임시저장을 다시 열 화면이 배선되기 전까지 `AfternoteEditorRoute` 가 `null` 을 넘긴다.
     * 그때 버튼이 남아 있으면 «누르면 홈에서 사라지고 되찾을 길이 없는» 상태가 된다.
     */
    @Test
    fun `editor top bar hides the draft action when no callback is wired`() {
        composeRule.setContent {
            AfternoteTheme {
                AfternoteEditorScreen(
                    form = EditorFormState(),
                    onBackClick = {},
                    onSaveDraftClick = null,
                    onRegisterClick = {},
                    snackbarMessage = null,
                    onSnackbarMessageConsumed = {},
                    validationMessage = null,
                    onValidationMessageConsumed = {},
                    content = {},
                )
            }
        }

        composeRule.assertAccessibleClickTargets()
        val names = composeRule.scanEnabledClickTargets().map { it.name }
        assertEquals(emptyList<String>(), names.filter { it == "임시저장" })
        assertEquals(1, names.count { it == "등록" })
    }
}
