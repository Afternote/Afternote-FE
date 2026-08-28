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
import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.ReceivedAccountCredentials
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.model.ReceivedExportBundle
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.receiver.presentation.deliveryverification.MasterKeyScreen
import com.afternote.feature.receiver.presentation.deliveryverification.MasterKeyViewModel
import com.afternote.feature.receiver.presentation.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.receiver.presentation.detail.ReceivedAfternoteDetailViewModel
import com.afternote.feature.receiver.presentation.recordsbox.ReceivedRecordsScreen
import com.afternote.feature.receiver.presentation.recordsbox.ReceivedRecordsViewModel
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistrationScreen
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistrationViewModel
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry
import com.afternote.feature.receiver.presentation.summary.ReceiverAfterNoteScreen
import com.afternote.feature.receiver.presentation.summary.ReceiverDownloadAllEvent
import com.afternote.feature.receiver.presentation.summary.ReceiverDownloadAllViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.afternote.feature.afternote.presentation.R as AfternoteFeatureR

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
        val receiverRepository = AdvancedReceiverRepository()
        val authRepository = AdvancedReceiverAuthRepository()
        authRepository.masterKeyResults.addLast(
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
        assertEquals(listOf(normalizedMasterKey), receiverRepository.savedAuthCodes)
        assertEquals(normalizedMasterKey, receiverRepository.authCode.value)
        assertEquals(normalizedMasterKey, attached.authCode)
        assertEquals("이발신", attached.realSenderName)
        assertEquals("가족", attached.relation)
        assertEquals(1, verifiedTransitions)
    }

    @Test
    fun receivedDetail_failureThenRetry_showsRecoveredDetailAndCallsSameIdTwice() {
        val repository = AdvancedReceiverRepository()
        repository.detailResults.addLast(Result.failure(IllegalStateException("offline")))
        repository.detailResults.addLast(Result.success(receivedSocialDetail()))
        val viewModel =
            ReceivedAfternoteDetailViewModel(
                savedStateHandle = SavedStateHandle(mapOf("afternoteId" to 91L)),
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

        assertEquals(listOf(91L, 91L), repository.detailIds)
    }

    @Test
    fun download_cancelThenDownloadFailureSaveFailureAndSuccess_preservesStageBoundaries() {
        val repository = AdvancedReceiverRepository()
        val saveFailureBundle = ReceivedExportBundle(payloadJson = "{\"attempt\":2}")
        val successBundle = ReceivedExportBundle(payloadJson = "{\"attempt\":3}")
        repository.downloadResults.addLast(Result.failure(IllegalStateException("offline")))
        repository.downloadResults.addLast(Result.success(saveFailureBundle))
        repository.downloadResults.addLast(Result.success(successBundle))
        repository.saveResults.addLast(Result.failure(IllegalStateException("disk full")))
        repository.saveResults.addLast(Result.success(Unit))
        val viewModel = ReceiverDownloadAllViewModel(repository, FakeErrorReporter())

        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AfternoteTheme {
                ReceiverAfterNoteScreen(
                    downloadUiState = uiState,
                    showBottomBar = false,
                    onDownloadConfirm = {
                        viewModel.onEvent(ReceiverDownloadAllEvent.ConfirmDownload)
                    },
                )
            }
        }

        openDownloadDialog()
        composeRule.onNodeWithText("아니요").performClick()
        assertEquals(0, repository.downloadCalls)
        assertEquals(0, repository.savedBundles.size)

        confirmDownload()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.errorMessageRes == AfternoteFeatureR.string.receiver_download_all_failed
        }
        assertEquals(1, repository.downloadCalls)
        assertEquals(0, repository.savedBundles.size)

        confirmDownload()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.errorMessageRes == AfternoteFeatureR.string.receiver_download_all_save_failed
        }
        assertEquals(2, repository.downloadCalls)
        assertEquals(listOf(saveFailureBundle), repository.savedBundles)

        confirmDownload()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.downloadSuccess
        }
        assertEquals(3, repository.downloadCalls)
        assertEquals(listOf(saveFailureBundle, successBundle), repository.savedBundles)
        assertNull(viewModel.uiState.value.errorMessageRes)
        assertTrue(viewModel.uiState.value.downloadSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private fun openDownloadDialog() {
        composeRule
            .onNodeWithText("모든 기록 내려받기")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("모든 기록을 내려받으시겠습니까?").assertIsDisplayed()
    }

    private fun confirmDownload() {
        openDownloadDialog()
        composeRule.onNodeWithText("예").performClick()
    }
}

private enum class RegistrationPhase {
    REGISTRATION,
    RECORDS,
    MASTER_KEY,
}

private class AdvancedReceiverRepository : ReceiverRepository {
    val authCode = MutableStateFlow<String?>(null)
    override val authCodeFlow: Flow<String?> = authCode
    val savedAuthCodes = mutableListOf<String>()
    val detailIds = mutableListOf<Long>()
    val detailResults = ArrayDeque<Result<ReceivedAfternoteDetail>>()
    val downloadResults = ArrayDeque<Result<ReceivedExportBundle>>()
    val saveResults = ArrayDeque<Result<Unit>>()
    val savedBundles = mutableListOf<ReceivedExportBundle>()
    var downloadCalls = 0

    override suspend fun currentAuthCode(): String? = authCode.value

    override suspend fun saveAuthCode(code: String) {
        savedAuthCodes += code
        authCode.value = code
    }

    override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> = flowOf(PagingData.empty())

    override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> =
        Result.success(AfterNotesListResult(items = emptyList(), totalCount = 0))

    override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> {
        detailIds += afternoteId
        return detailResults.removeFirst()
    }

    override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> {
        downloadCalls += 1
        return downloadResults.removeFirst()
    }

    override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> {
        savedBundles += bundle
        return saveResults.removeFirst()
    }

    override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> = Result.success(null)
}

private class AdvancedReceiverAuthRepository : ReceiverAuthRepository {
    val masterKeyResults = ArrayDeque<Result<ReceiverIdentity>>()
    val verifiedMasterKeys = mutableListOf<String>()

    override suspend fun verifyMasterKey(authCode: String): Result<ReceiverIdentity> {
        verifiedMasterKeys += authCode
        return masterKeyResults.removeFirst()
    }

    override suspend fun sendEmailAuthCode(email: String): Result<Unit> = error("unexpected sendEmailAuthCode")

    override suspend fun verifyEmailAuthCode(
        email: String,
        authCode: String,
    ): Result<ReceiverEmailAuthResult> = error("unexpected verifyEmailAuthCode")

    override suspend fun getPresignedUrl(
        extension: String,
        contentLength: Long,
    ): Result<ReceiverAuthPresignedUrl> = error("unexpected getPresignedUrl")

    override suspend fun submitDeliveryVerification(
        deathCertificateUrl: String?,
        familyRelationCertificateUrl: String?,
    ): Result<DeliveryVerification> = error("unexpected submitDeliveryVerification")

    override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> = error("unexpected getDeliveryVerificationStatus")

    override suspend fun getSenderMessage(): Result<SenderMessageInfo> = error("unexpected getSenderMessage")
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
