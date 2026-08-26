package com.afternote.afternote_fe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeVoiceRecorderRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.domain.usecase.CreateTimeLetterUseCase
import com.afternote.feature.timeletter.domain.usecase.ResolveTimeLetterBlocksUseCase
import com.afternote.feature.timeletter.presentation.screen.recipient.RecipientTimeLetterDetailScreen
import com.afternote.feature.timeletter.presentation.screen.recipient.RecipientTimeletterScreen
import com.afternote.feature.timeletter.presentation.screen.sender.DraftLetterScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterWriteScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen
import com.afternote.feature.timeletter.presentation.viewmodel.DraftLetterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.DraftLetterViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeLetterDetailUiState
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeLetterDetailViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeletterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientTimeletterViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimeLetterLifecycleAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun senderList_loadingErrorSuccessFilterAndDeleteRetry_keepRepositoryBoundary() {
        val repository = PrivateTimeLetterRepository()
        repository.deleteResults.addLast(Result.failure(IllegalStateException("delete failed")))
        repository.deleteResults.addLast(Result.success(Unit))
        val firstLoad = CompletableDeferred<Result<TimeLetterList>>()
        repository.nextListLoad = firstLoad
        val userRepository = privateUserRepository(testReceivers)
        val viewModel = TimeletterViewModel(repository, userRepository)

        composeRule.setContent {
            AfternoteTheme {
                TimeletterScreen(viewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) { repository.listCalls == 1 }
        assertEquals(TimeletterUiState.Loading, viewModel.uiState.value)

        firstLoad.complete(Result.failure(IllegalStateException("offline")))
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { viewModel.uiState.value == TimeletterUiState.Error }
        composeRule
            .onNodeWithText("아직 등록된 타임레터가 없어요.", substring = true)
            .assertIsDisplayed()

        val visibleLetters =
            TimeLetterList(
                timeLetters =
                    listOf(
                        timeLetter(id = 11L, title = "김수신에게", receiverIds = listOf(7L)),
                        timeLetter(id = 12L, title = "박친구에게", receiverIds = listOf(8L)),
                    ),
                totalCount = 2,
            )
        val retryLoad = CompletableDeferred<Result<TimeLetterList>>()
        composeRule.runOnIdle {
            repository.nextListLoad = retryLoad
            viewModel.load()
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { viewModel.uiState.value == TimeletterUiState.Loading }
        retryLoad.complete(Result.success(visibleLetters))

        composeRule.onNodeWithText("김수신에게").assertIsDisplayed()
        composeRule.onNodeWithText("박친구에게").assertIsDisplayed()
        composeRule.runOnIdle { viewModel.setReceiverFilter(listOf(7L)) }

        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onNodeWithText("김수신에게").assertIsDisplayed()
        composeRule.onNodeWithText("박친구에게").assertDoesNotExist()
        assertEquals(2, repository.listCalls)
        assertEquals(2, userRepository.getReceiversCalls)
        val success = viewModel.uiState.value as TimeletterUiState.Success
        assertEquals(setOf(7L), success.selectedFilterReceiverIds)
        assertEquals(listOf(11L), success.letters.timeLetters.map(TimeLetter::id))

        deleteVisibleLetter()
        composeRule
            .onNodeWithText("타임레터를 삭제하지 못했습니다. 다시 시도해 주세요.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("김수신에게").assertIsDisplayed()
        assertEquals(listOf(listOf(11L)), repository.deleteCalls)

        deleteVisibleLetter()
        composeRule
            .onNodeWithText("아직 등록된 타임레터가 없어요.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("김수신에게").assertDoesNotExist()
        assertEquals(listOf(listOf(11L), listOf(11L)), repository.deleteCalls)
        assertEquals(3, repository.listCalls)
        assertEquals(3, userRepository.getReceiversCalls)
    }

    @Test
    fun drafts_selectionDeleteReentryAndDeleteAll_reloadDurableRepositoryState() {
        val firstDraft = timeLetter(id = 31L, title = "첫 임시 편지", status = TimeLetterStatus.DRAFT)
        val secondDraft = timeLetter(id = 32L, title = "둘째 임시 편지", status = TimeLetterStatus.DRAFT)
        val repository =
            PrivateTimeLetterRepository(
                draftLetters = TimeLetterList(listOf(firstDraft, secondDraft), totalCount = 2),
            )
        var activeViewModel by mutableStateOf(DraftLetterViewModel(repository))

        composeRule.setContent {
            AfternoteTheme {
                DraftLetterScreen(onBackClick = {}, viewModel = activeViewModel)
            }
        }
        composeRule.onNodeWithText("첫 임시 편지").assertIsDisplayed()
        composeRule.onNodeWithText("둘째 임시 편지").assertIsDisplayed()

        composeRule.onNodeWithText("수정").performClick()
        composeRule.onNode(hasText("첫 임시 편지") and hasClickAction()).performClick()
        composeRule.onNodeWithText("삭제").performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { repository.deleteCalls == listOf(listOf(31L)) }
        composeRule.onNodeWithText("첫 임시 편지").assertDoesNotExist()
        composeRule.onNodeWithText("둘째 임시 편지").assertIsDisplayed()

        composeRule.runOnIdle { activeViewModel = DraftLetterViewModel(repository) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { repository.temporaryListCalls == 2 }
        composeRule.onNodeWithText("첫 임시 편지").assertDoesNotExist()
        composeRule.onNodeWithText("둘째 임시 편지").assertIsDisplayed()

        composeRule.runOnIdle { activeViewModel.deleteAll() }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            val state = activeViewModel.uiState.value
            state is DraftLetterUiState.Success && state.drafts.isEmpty()
        }
        composeRule.onNodeWithText("둘째 임시 편지").assertDoesNotExist()
        assertEquals(1, repository.deleteAllTemporaryCalls)
        assertTrue(repository.draftLetters.timeLetters.isEmpty())
    }

    @Test
    fun editingExistingLetter_showsLoadedStateAndSendsExactUpdatePayload() {
        val existingLetter =
            timeLetter(
                id = 41L,
                title = "기존 제목",
                receiverIds = listOf(7L),
                sendAt = "2026-09-04T15:20:00",
                blocks =
                    listOf(
                        TimeLetterBlock(
                            id = 410L,
                            blockType = TimeLetterBlockType.TEXT,
                            blockOrder = 1,
                            textContent = "기존 본문",
                            url = null,
                            mimeType = null,
                        ),
                    ),
            )
        val repository = PrivateTimeLetterRepository(editingLetter = existingLetter)
        val viewModel = writeViewModel(repository, privateUserRepository(testReceivers), timeLetterId = 41L)

        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AfternoteTheme {
                TimeLetterWriteScreen(
                    uiState = uiState,
                    onRegisterClick = viewModel::register,
                    onErrorShown = viewModel::clearError,
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.editingTimeLetterId == 41L &&
                !viewModel.uiState.value.isLoadingEditingLetter
        }
        composeRule.onNodeWithText("기존 제목").assertIsDisplayed()
        composeRule.onNodeWithText("기존 본문").assertIsDisplayed()
        composeRule.onNodeWithText("김수신 님에게").assertIsDisplayed()
        composeRule.onNodeWithText("2026-09-04").assertIsDisplayed()

        composeRule.runOnIdle {
            viewModel.register(
                title = "수정 제목",
                textContents = mapOf(410L to "수정 본문"),
            )
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { viewModel.uiState.value.registered }

        val call = repository.updateCalls.single()
        assertEquals(41L, call.timeLetterId)
        assertEquals("수정 제목", call.title)
        assertEquals("2026-09-04T15:20:00", call.sendAt)
        assertEquals(TimeLetterDeliveryMode.DATE, call.deliveryMode)
        assertEquals(TimeLetterStatus.SCHEDULED, call.status)
        assertEquals(
            listOf(
                NewTimeLetterBlock(
                    blockType = TimeLetterBlockType.TEXT,
                    blockOrder = 1,
                    textContent = "수정 본문",
                ),
            ),
            call.blocks,
        )
        assertEquals(1, repository.detailCalls)
        assertEquals(0, repository.listCalls)
        assertEquals(0, repository.createCalls)
        assertEquals(listOf(7L), viewModel.uiState.value.recipientIds)
    }

    @Test
    fun recipientListAndDetail_errorRetry_recoversBothRepositoryBoundaries() {
        val receivedLetter = receivedTimeLetter()
        val repository = PrivateReceiverTimeLetterRepository()
        repository.listResults.addLast(Result.failure(IllegalStateException("list offline")))
        repository.listResults.addLast(Result.success(ReceivedTimeLetterList(listOf(receivedLetter), 1)))
        repository.detailResults.addLast(Result.failure(IllegalStateException("detail offline")))
        repository.detailResults.addLast(Result.success(receivedLetter))
        val listViewModel = RecipientTimeletterViewModel(repository)
        var detailViewModel by mutableStateOf<RecipientTimeLetterDetailViewModel?>(null)
        var showDetail by mutableStateOf(false)

        composeRule.setContent {
            AfternoteTheme {
                if (showDetail) {
                    RecipientTimeLetterDetailScreen(
                        onBackClick = {},
                        viewModel = requireNotNull(detailViewModel),
                    )
                } else {
                    RecipientTimeletterScreen(viewModel = listViewModel)
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            listViewModel.uiState.value == RecipientTimeletterUiState.Error
        }
        composeRule.onNodeWithText("타임레터를 불러올 수 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").performClick()
        composeRule.onNodeWithText("받은 편지").assertIsDisplayed()
        assertEquals(2, repository.listCalls)

        composeRule.runOnIdle {
            detailViewModel =
                RecipientTimeLetterDetailViewModel(
                    receiverTimeLetterRepository = repository,
                    savedStateHandle = SavedStateHandle(mapOf("timeLetterReceiverId" to 71L)),
                )
            showDetail = true
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            detailViewModel?.uiState?.value == RecipientTimeLetterDetailUiState.Error
        }
        composeRule.onNodeWithText("타임레터를 불러올 수 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").performClick()
        composeRule.onNodeWithText("받은 편지").assertIsDisplayed()
        composeRule.onNodeWithText("복구된 상세 본문").assertIsDisplayed()
        assertEquals(listOf(71L, 71L), repository.detailIds)
    }

    private fun deleteVisibleLetter() {
        composeRule.onNodeWithContentDescription("더보기 설정").performClick()
        composeRule.onNodeWithText("삭제").performClick()
        composeRule.onNodeWithText("예").performClick()
    }

    private fun writeViewModel(
        repository: PrivateTimeLetterRepository,
        userRepository: UserRepository,
        timeLetterId: Long,
    ): TimeLetterWriteViewModel {
        val resolver = ResolveTimeLetterBlocksUseCase(FakePhotoUploadRepository.strict())
        return TimeLetterWriteViewModel(
            createTimeLetterUseCase = CreateTimeLetterUseCase(repository, resolver),
            resolveTimeLetterBlocksUseCase = resolver,
            timeLetterRepository = repository,
            userRepository = userRepository,
            fileMetadataRepository = PrivateFileMetadataRepository,
            voiceRecorderRepository = FakeVoiceRecorderRepository,
            savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to timeLetterId)),
        )
    }

    private companion object {
        const val TIMEOUT = 5_000L

        val testReceivers =
            listOf(
                Receiver(receiverId = 7L, name = "김수신", relation = "가족", authCode = "auth-7"),
                Receiver(receiverId = 8L, name = "박친구", relation = "친구", authCode = "auth-8"),
            )
    }
}

private data class PrivateTimeLetterUpdateCall(
    val timeLetterId: Long,
    val title: String?,
    val blocks: List<NewTimeLetterBlock>,
    val sendAt: String?,
    val deliveryMode: TimeLetterDeliveryMode?,
    val status: TimeLetterStatus?,
)

private class PrivateTimeLetterRepository(
    registeredLetters: TimeLetterList = TimeLetterList(emptyList(), 0),
    draftLetters: TimeLetterList = TimeLetterList(emptyList(), 0),
    private val editingLetter: TimeLetter? = null,
) : TimeLetterRepository {
    var registeredLetters = registeredLetters
        private set
    var draftLetters = draftLetters
        private set
    var nextListLoad: CompletableDeferred<Result<TimeLetterList>>? = null
    val deleteResults = ArrayDeque<Result<Unit>>()
    val deleteCalls = mutableListOf<List<Long>>()
    val updateCalls = mutableListOf<PrivateTimeLetterUpdateCall>()
    var listCalls = 0
        private set
    var temporaryListCalls = 0
        private set
    var detailCalls = 0
        private set
    var createCalls = 0
        private set
    var deleteAllTemporaryCalls = 0
        private set

    override suspend fun getTimeLetters(): TimeLetterList {
        listCalls += 1
        val pending = nextListLoad
        nextListLoad = null
        val loaded = pending?.await()?.getOrThrow() ?: registeredLetters
        registeredLetters = loaded
        return loaded
    }

    override suspend fun getTemporaryTimeLetters(): TimeLetterList {
        temporaryListCalls += 1
        return draftLetters
    }

    override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter {
        detailCalls += 1
        return requireNotNull(editingLetter).also { assertEquals(timeLetterId, it.id) }
    }

    override suspend fun createTimeLetter(
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode,
        status: TimeLetterStatus,
        receiverIds: List<Long>,
    ): TimeLetter {
        createCalls += 1
        return timeLetter(
            id = 999L,
            title = title,
            receiverIds = receiverIds,
            sendAt = sendAt,
            status = status,
        )
    }

    override suspend fun updateTimeLetter(
        timeLetterId: Long,
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode?,
        status: TimeLetterStatus?,
    ): TimeLetter {
        updateCalls +=
            PrivateTimeLetterUpdateCall(
                timeLetterId = timeLetterId,
                title = title,
                blocks = blocks,
                sendAt = sendAt,
                deliveryMode = deliveryMode,
                status = status,
            )
        return requireNotNull(editingLetter).copy(title = title, sendAt = sendAt)
    }

    override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) {
        deleteCalls += timeLetterIds
        val result = deleteResults.removeFirstOrNull() ?: Result.success(Unit)
        result.getOrThrow()
        registeredLetters = registeredLetters.without(timeLetterIds)
        draftLetters = draftLetters.without(timeLetterIds)
    }

    override suspend fun deleteAllTemporary() {
        deleteAllTemporaryCalls += 1
        draftLetters = TimeLetterList(emptyList(), 0)
    }
}

private class PrivateReceiverTimeLetterRepository : ReceiverTimeLetterRepository {
    val listResults = ArrayDeque<Result<ReceivedTimeLetterList>>()
    val detailResults = ArrayDeque<Result<ReceivedTimeLetter>>()
    val detailIds = mutableListOf<Long>()
    var listCalls = 0
        private set

    override suspend fun getReceivedTimeLetters(): ReceivedTimeLetterList {
        listCalls += 1
        return requireNotNull(listResults.removeFirstOrNull()).getOrThrow()
    }

    override suspend fun getReceivedTimeLetterDetail(timeLetterReceiverId: Long): ReceivedTimeLetter {
        detailIds += timeLetterReceiverId
        return requireNotNull(detailResults.removeFirstOrNull()).getOrThrow()
    }
}

private object PrivateFileMetadataRepository : FileMetadataRepository {
    override suspend fun getFileName(uriString: String): String = error("File metadata is not expected")

    override suspend fun getMimeType(uriString: String): String? = error("File metadata is not expected")
}

private fun privateUserRepository(receivers: List<Receiver>): FakeUserRepository =
    FakeUserRepository.strict().apply {
        onReceiverListFlow = { flowOf(receivers) }
        onGetReceivers = { receivers }
    }

private fun TimeLetterList.without(ids: List<Long>): TimeLetterList {
    val remaining = timeLetters.filterNot { it.id in ids }
    return copy(timeLetters = remaining, totalCount = remaining.size)
}

private fun timeLetter(
    id: Long,
    title: String?,
    receiverIds: List<Long> = emptyList(),
    sendAt: String? = "2026-10-01T10:00:00",
    status: TimeLetterStatus = TimeLetterStatus.SCHEDULED,
    blocks: List<TimeLetterBlock> =
        listOf(
            TimeLetterBlock(
                id = id * 10,
                blockType = TimeLetterBlockType.TEXT,
                blockOrder = 1,
                textContent = "본문 $id",
                url = null,
                mimeType = null,
            ),
        ),
): TimeLetter =
    TimeLetter(
        id = id,
        title = title,
        sendAt = sendAt,
        deliveredAt = null,
        status = status,
        blocks = blocks,
        receiverIds = receiverIds,
    )

private fun receivedTimeLetter(): ReceivedTimeLetter =
    ReceivedTimeLetter(
        id = 70L,
        timeLetterReceiverId = 71L,
        title = "받은 편지",
        blocks =
            listOf(
                TimeLetterBlock(
                    id = 72L,
                    blockType = TimeLetterBlockType.TEXT,
                    blockOrder = 1,
                    textContent = "복구된 상세 본문",
                    url = null,
                    mimeType = null,
                ),
            ),
        sendAt = "2026-08-22T09:00:00",
        status = TimeLetterStatus.SENT,
        senderName = "테스트 발신인",
        deliveredAt = "2026-08-22T09:00:00",
        createdAt = "2026-08-20T09:00:00",
        isRead = false,
    )
