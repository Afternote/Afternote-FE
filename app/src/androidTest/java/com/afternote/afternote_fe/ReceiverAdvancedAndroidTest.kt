package com.afternote.afternote_fe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.afternote.feature.receiver.domain.model.ReceivedExportBundle
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import com.afternote.feature.receiver.presentation.deliveryverification.MasterKeyScreen
import com.afternote.feature.receiver.presentation.deliveryverification.MasterKeyViewModel
import com.afternote.feature.receiver.presentation.recordsbox.ReceivedRecordsScreen
import com.afternote.feature.receiver.presentation.recordsbox.ReceivedRecordsViewModel
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistrationScreen
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistrationViewModel
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.afternote.feature.receiver.presentation.R as ReceiverR

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
    fun senderRegistration_recordsEntryAndMasterKey_shareExactReceiverContext() {
        val senderRegistry = SenderRegistry()
        val registrationViewModel = SenderRegistrationViewModel(senderRegistry)
        val recordsViewModel = ReceivedRecordsViewModel(senderRegistry)
        val masterKeyResults = ArrayDeque<Result<ReceiverIdentity>>()
        val receiverRepository =
            FakeReceiverRepository.strict().apply {
                onSaveMasterKey = { masterKeyState.value = it }
            }
        val authRepository =
            FakeReceiverAuthRepository.strict().apply {
                onVerifyMasterKey = { masterKeyResults.removeFirst() }
            }
        masterKeyResults.addLast(
            Result.success(
                ReceiverIdentity(
                    receiverId = 7L,
                    receiverName = "김수신",
                    senderName = "이발신",
                    relation = "가족",
                ),
            ),
        )
        val masterKeyViewModel =
            MasterKeyViewModel(
                senderRegistry = senderRegistry,
                receiverRepository = receiverRepository,
                receiverAuthRepository = authRepository,
                errorReporter = FakeErrorReporter(),
            )
        var phase by mutableStateOf(RegistrationPhase.REGISTRATION)
        var selectedSenderId: String? = null
        var registeredTransitions = 0
        var verifiedTransitions = 0

        composeRule.setContent {
            AfternoteTheme {
                when (phase) {
                    RegistrationPhase.REGISTRATION -> {
                        SenderRegistrationScreen(
                            onBackClick = {},
                            onRegistered = {
                                registeredTransitions += 1
                                phase = RegistrationPhase.RECORDS
                            },
                            viewModel = registrationViewModel,
                        )
                    }

                    RegistrationPhase.RECORDS -> {
                        ReceivedRecordsScreen(
                            onBackClick = {},
                            onAddSenderClick = {},
                            onSenderClick = { sender ->
                                selectedSenderId = sender.id
                                phase = RegistrationPhase.MASTER_KEY
                            },
                            viewModel = recordsViewModel,
                        )
                    }

                    RegistrationPhase.MASTER_KEY -> {
                        MasterKeyScreen(
                            senderId = checkNotNull(selectedSenderId),
                            onBackClick = {},
                            onVerified = { verifiedTransitions += 1 },
                            viewModel = masterKeyViewModel,
                        )
                    }
                }
            }
        }

        val registerButton = hasText("발신자 등록하기") and hasClickAction()
        composeRule.onNode(registerButton).assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextInput("  가족 별칭  ")
        composeRule.onNode(registerButton).assertIsEnabled().performClick()

        composeRule.onNodeWithText("받은 기록함").assertIsDisplayed()
        composeRule.onNodeWithText("가족 별칭").assertIsDisplayed().performClick()
        val sender = senderRegistry.senders.value.single()
        assertEquals(1, registeredTransitions)
        assertEquals("가족 별칭", sender.name)
        assertEquals(sender.id, selectedSenderId)

        composeRule
            .onNode(hasText("마스터 키 입력") and hasSetTextAction())
            .assertIsDisplayed()
        val nextButton = hasText("다음") and hasClickAction()
        composeRule.onNode(nextButton).assertIsNotEnabled()
        // 마스터 키는 UUID 형식만 통과하고 소문자로 정규화된다(#887) — 공백·대문자 입력으로 함께 검증.
        composeRule.onNode(hasSetTextAction()).performTextInput("  3F2504E0-4F89-11D3-9A0C-0305E82C3301  ")
        composeRule.onNode(nextButton).assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { verifiedTransitions == 1 }

        val normalizedMasterKey = "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
        val attached = checkNotNull(senderRegistry.findById(sender.id))
        assertEquals(listOf(normalizedMasterKey), authRepository.verifiedMasterKeys)
        assertEquals(listOf(normalizedMasterKey), receiverRepository.savedMasterKeys)
        assertEquals(normalizedMasterKey, receiverRepository.masterKeyState.value)
        assertEquals(normalizedMasterKey, attached.masterKey)
        assertEquals("이발신", attached.realSenderName)
        assertEquals("가족", attached.relation)
        assertEquals(1, verifiedTransitions)
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

private enum class RegistrationPhase {
    REGISTRATION,
    RECORDS,
    MASTER_KEY,
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
