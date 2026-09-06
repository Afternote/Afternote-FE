package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.appTestUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.presentation.screen.sender.RecipientListScreen
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientListViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 수신인 목록 화면은 저장소가 내는 목록을 그대로 따른다 — 세션이 바뀌어 목록이 비면 이전 계정의
 * 식별자가 화면에 남지 않고 화면도 살아 있어야 한다.
 *
 * «로그아웃·새 세션의 401 을 예외로 흘리지 않고 빈 목록으로 낮춘다» 는 판정은 저장소 구현의 계약이라
 * `:core:data` 단위 테스트(`UserRepositoryImplTest` 의 receiverListFlow 계열)가 고정한다. 여기서는
 * 그 결과를 fake 로 흉내 내 화면 쪽 경계만 본다.
 */
@RunWith(AndroidJUnit4::class)
class ReceiverSessionIsolationAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun recipientList_receiverListEmptiesOnSessionChange_keepsScreenAliveWithoutPreviousReceiverIdentity() {
        val repository = appTestUserRepository(receivers = listOf(previousAccountReceiver()))
        val viewModel = RecipientListViewModel(repository)

        composeRule.setContent {
            AfternoteTheme {
                RecipientListScreen(
                    onBackClick = {},
                    onConfirmClick = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.recipients.value.any { it.name == PREVIOUS_ACCOUNT_RECEIVER }
        }
        composeRule.onNodeWithText(PREVIOUS_ACCOUNT_RECEIVER).assertIsDisplayed()

        composeRule.runOnIdle { repository.receiverState.value = emptyList() }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            viewModel.recipients.value.isEmpty()
        }

        composeRule.onNodeWithText("수신인 목록").assertIsDisplayed()
        composeRule.onNodeWithText(PREVIOUS_ACCOUNT_RECEIVER).assertDoesNotExist()
    }

    private companion object {
        const val PREVIOUS_ACCOUNT_RECEIVER = "이전 계정 수신인"
        const val TIMEOUT_MILLIS = 5_000L

        fun previousAccountReceiver() = Receiver(91L, PREVIOUS_ACCOUNT_RECEIVER, "가족", "previous-account-auth")
    }
}
