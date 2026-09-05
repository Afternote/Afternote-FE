package com.afternote.afternote_fe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 처리 방법 입력 칸 여닫힘 실기 검증 (이슈 #777).
 *
 * [com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodListState]
 * 단위 테스트는 «닫는 경로가 있다» 까지만 말한다. 그 경로에 AddItemTextField 의 신호가 실제로
 * 닿는지는 상호작용을 거쳐야 드러나므로 여기서 본다 — 종전에는 그 신호가
 * `ProcessingMethodSection.onTextFieldVisibilityChanged = {}` 로 나가 사라졌고, 항목만 추가되고
 * 빈 입력 칸이 열린 채 남았다.
 *
 * 서버가 필요 없다 — 리스트 컴포저블과 로컬 상태만으로 재현된다.
 */
@RunWith(AndroidJUnit4::class)
class ProcessingMethodListAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addItemTextField_afterItemAdded_closesInputField() {
        composeRule.setContent {
            AfternoteTheme {
                var items by remember { mutableStateOf(emptyList<ProcessingMethodItem>()) }
                ProcessingMethodList(
                    items = items,
                    onItemAdded = { text -> items = items + ProcessingMethodItem(localId = items.size, text = text) },
                    onItemDeleteClick = {},
                    onItemEdited = { _, _ -> },
                    initialShowTextField = true,
                )
            }
        }

        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("게시물 내리기")
        composeRule.onAllNodes(hasSetTextAction())[0].performImeAction()

        composeRule.onNodeWithText("게시물 내리기").assertIsDisplayed()
        // 핵심: 추가 뒤 입력 칸이 남아 있으면 안 된다.
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }
}
