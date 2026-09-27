package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientListUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 1hyok/Sadturtleman 님 리뷰 지적 — 백그라운드 5초 이상 뒤 재구독으로 uiState 가 Loading 을 거쳐
 * Success 로 돌아올 때(WhileSubscribed(5_000)) 체크해 둔 수신인 선택이 사라지던 회귀(#714)의 검증.
 * RecipientListScreen 은 이 순환과 무관하게 계속 컴포지션에 남아 있어야 하고, 선택은 그 화면
 * 최상단으로 옮겨졌으니 살아남아야 한다 — uiState 를 직접 갈아끼워 그 순환만 재현한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RecipientListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `uiState가 Loading을 거쳐 다시 Success가 되어도 체크해 둔 수신인 선택이 유지된다`() {
        val recipients =
            listOf(
                ReceiverListItem(receiverId = 1L, name = "김수신", relation = "가족"),
                ReceiverListItem(receiverId = 2L, name = "이수신", relation = "친구"),
            )
        var uiState by mutableStateOf<RecipientListUiState>(RecipientListUiState.Success(recipients))
        var confirmed: List<ReceiverListItem>? = null

        composeRule.setContent {
            AfternoteTheme {
                RecipientListScreen(
                    uiState = uiState,
                    onBackClick = {},
                    onConfirmClick = { confirmed = it },
                    onRetry = {},
                )
            }
        }

        composeRule
            .onNodeWithTag("recipient_item_1", useUnmergedTree = true)
            .onChildren()
            .filterToOne(hasClickAction())
            .performClick()

        // WhileSubscribed(5_000) 재구독이 uiState 에 만드는 것과 같은 Loading→Success 순환.
        composeRule.runOnIdle { uiState = RecipientListUiState.Loading }
        composeRule.waitForIdle()
        composeRule.runOnIdle { uiState = RecipientListUiState.Success(recipients) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()

        assertEquals(listOf(1L), confirmed?.map { it.receiverId })
    }
}
