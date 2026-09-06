package com.afternote.core.ui.receiver

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [ReceiverSelectScreen] 소비자 계약 가드 (#791).
 *
 * 세부 선택 동작(검색 유지·단일 교체 등)은 설정 소비 경로의
 * `ReceiverSelectionTest`·`ReceiverSelectionBoundaryTest` 가 전환 전 그대로 검증한다 —
 * 여기서는 공용 API 고유 계약(완료 id 전달, 행 전체 탭, listReplacement)만 고정한다.
 *
 * 복수 선택 오버로드(#1426)와 단일 선택 오버로드가 같은 그림·같은 완료 버튼 활성 조건을
 * 공유하는 것도 여기서 함께 고정한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReceiverSelectScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val receivers =
        listOf(
            ReceiverSelectItem(id = 7L, name = "김수신", relation = "가족"),
            ReceiverSelectItem(id = 11L, name = "박친구", relation = "친구"),
        )

    @Test
    fun `완료 클릭은 현재 선택된 id 를 그대로 전달한다`() {
        var confirmed: Long? = null
        setContent(selectedReceiverId = 11L, onConfirmClick = { confirmed = it })

        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()

        assertEquals(11L, confirmed)
    }

    @Test
    fun `선택이 없으면 완료 버튼은 비활성이고 콜백이 나가지 않는다`() {
        var confirmCalls = 0
        setContent(selectedReceiverId = null, onConfirmClick = { confirmCalls += 1 })

        composeRule.onNodeWithText("수신자 선택 완료하기").assertIsNotEnabled()

        assertEquals(0, confirmCalls)
    }

    @Test
    fun `행의 이름 영역을 탭해도 해당 수신자 id 로 토글 콜백이 온다`() {
        val toggled = mutableListOf<Long>()
        setContent(selectedReceiverId = null, onReceiverToggle = { toggled += it })

        composeRule.onNodeWithText("김수신").performClick()

        assertEquals(listOf(7L), toggled)
    }

    @Test
    fun `listReplacement 는 목록만 대체하고 검색 필드와 완료 버튼은 남긴다`() {
        setContent(
            selectedReceiverId = null,
            listReplacement = { Text("불러오는 중 표시") },
        )

        composeRule.onNodeWithText("불러오는 중 표시").assertIsDisplayed()
        composeRule.onNodeWithText("김수신").assertDoesNotExist()
        composeRule.onNodeWithText("이름으로 검색하기").assertIsDisplayed()
        composeRule.onNodeWithText("수신자 선택 완료하기").assertIsDisplayed()
    }

    @Test
    fun `검색은 이름 부분 일치로 목록을 거른다`() {
        setContent(selectedReceiverId = null)

        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("김")

        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onNodeWithText("박친구").assertDoesNotExist()
    }

    @Test
    fun `복수 선택 모드의 완료는 선택된 id 전체를 순서대로 전달한다`() {
        var confirmed: List<Long>? = null
        setMultiContent(selectedReceiverIds = listOf(11L, 7L), onConfirmClick = { confirmed = it })

        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()

        assertEquals(listOf(11L, 7L), confirmed)
    }

    @Test
    fun `복수 선택 모드에서 선택이 비면 완료 버튼은 비활성이고 콜백이 나가지 않는다`() {
        var confirmCalls = 0
        setMultiContent(selectedReceiverIds = emptyList(), onConfirmClick = { confirmCalls += 1 })

        composeRule.onNodeWithText("수신자 선택 완료하기").assertIsNotEnabled()

        assertEquals(0, confirmCalls)
    }

    @Test
    fun `복수 선택 모드는 선택된 모든 수신자를 선택 표시로 그린다`() {
        setMultiContent(selectedReceiverIds = listOf(7L, 11L))

        composeRule.onAllNodes(isSelected()).assertCountEquals(2)
        composeRule.onNode(isSelected() and hasText("김수신")).assertIsDisplayed()
        composeRule.onNode(isSelected() and hasText("박친구")).assertIsDisplayed()
    }

    @Test
    fun `복수 선택 모드에서 선택되지 않은 행은 선택 표시가 없다`() {
        setMultiContent(selectedReceiverIds = listOf(7L))

        composeRule.onAllNodes(isSelected()).assertCountEquals(1)
        composeRule.onNode(isSelected() and hasText("김수신")).assertIsDisplayed()
    }

    private fun setContent(
        selectedReceiverId: Long?,
        onReceiverToggle: (Long) -> Unit = {},
        onConfirmClick: (Long) -> Unit = {},
        listReplacement: (@androidx.compose.runtime.Composable () -> Unit)? = null,
    ) {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverSelectScreen(
                    receivers = receivers,
                    selectedReceiverId = selectedReceiverId,
                    onReceiverToggle = onReceiverToggle,
                    onBackClick = {},
                    onConfirmClick = onConfirmClick,
                    listReplacement = listReplacement,
                )
            }
        }
    }

    private fun setMultiContent(
        selectedReceiverIds: List<Long>,
        onReceiverToggle: (Long) -> Unit = {},
        onConfirmClick: (List<Long>) -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverSelectScreen(
                    receivers = receivers,
                    selectedReceiverIds = selectedReceiverIds,
                    onReceiverToggle = onReceiverToggle,
                    onBackClick = {},
                    onConfirmClick = onConfirmClick,
                )
            }
        }
    }
}
