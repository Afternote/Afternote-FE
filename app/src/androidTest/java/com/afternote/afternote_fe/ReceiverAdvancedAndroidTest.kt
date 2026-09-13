package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.receiver.navigation.ReceivedAfternoteRoute
import com.afternote.feature.receiver.domain.model.ReceivedAccountCredentials
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiverAdvancedAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun receivedDetail_failureThenRetry_showsRecoveredDetailAndCallsSameIdTwice() {
        val detailResults = ArrayDeque<Result<ReceivedAfternoteDetail>>()
        val repository =
            FakeReceiverRepository.strict().apply {
                onGetReceivedAfternoteDetail = { detailResults.removeFirst() }
            }
        detailResults.addLast(Result.failure(IllegalStateException("offline")))
        detailResults.addLast(Result.success(receivedSocialDetail()))
        val viewModel =
            ReceivedAfternoteDetailViewModel(
                route = ReceivedAfternoteRoute.DetailRoute(afternoteId = 91L),
                receiverRepository = repository,
                errorReporter = FakeErrorReporter(),
            )

        composeRule.setContent {
            AfternoteTheme {
                ReceivedAfternoteDetailRoute(
                    onNavigateBack = {},
                    onNavigateToFullList = {},
                    onNavigateToPlaylist = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("상세 정보를 불러오지 못했습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도하기").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("receiver@example.test").assertIsDisplayed()
        composeRule.onNodeWithText("표시").performClick()
        composeRule.onNodeWithText("receiver-password").assertIsDisplayed()

        assertEquals(listOf(91L, 91L), repository.requestedDetailIds)
    }
}

private fun receivedSocialDetail(): ReceivedAfternoteDetail =
    ReceivedAfternoteDetail(
        serviceName = "Instagram",
        senderName = "이발신",
        createdAt = "2026.08.22",
        type = AfternoteType.SOCIAL_NETWORK,
        processingMethods = listOf("계정 삭제"),
        leaveMessageBlocks =
            listOf(LeaveMessageBlock(title = "마지막 말", body = "기억해 줘")),
        credentials =
            ReceivedAccountCredentials(
                id = "receiver@example.test",
                password = "receiver-password",
            ),
    )
