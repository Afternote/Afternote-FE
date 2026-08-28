package com.afternote.feature.timeletter.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.afternote.feature.timeletter.domain.testing.FakeFileMetadataRepository
import com.afternote.feature.timeletter.domain.testing.FakeReceiverTimeLetterRepository
import com.afternote.feature.timeletter.domain.testing.FakeTimeLetterRepository
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TimeLetterLifecycleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun senderList_loadingErrorSuccessFilterAndDeleteRetry_keepRepositoryBoundary() {
        val repository = FakeTimeLetterRepository()
        val deleteResults =
            ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("delete failed")),
                    Result.success(Unit),
                ),
            )
        val firstLoad = CompletableDeferred<Result<TimeLetterList>>()
        var nextListLoad: CompletableDeferred<Result<TimeLetterList>>? = firstLoad
        repository.onGetTimeLetters = {
            val loaded =
                nextListLoad?.also { nextListLoad = null }?.await()?.getOrThrow()
                    ?: repository.registeredLetters
            repository.registeredLetters = loaded
            loaded
        }
        repository.onDeleteTimeLetters = { ids ->
            deleteResults.removeFirst().getOrThrow()
            ids.forEach(repository.details::remove)
            repository.registeredLetters = repository.registeredLetters.without(ids)
            repository.temporaryLetters = repository.temporaryLetters.without(ids)
        }
        val userRepository = privateUserRepository(testReceivers)
        val viewModel = TimeletterViewModel(repository, userRepository)

        composeRule.setContent {
            AfternoteTheme {
                TimeletterScreen(viewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) { repository.getTimeLettersCalls == 1 }
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
            nextListLoad = retryLoad
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
        assertEquals(2, repository.getTimeLettersCalls)
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
        assertEquals(3, repository.getTimeLettersCalls)
        assertEquals(3, userRepository.getReceiversCalls)
    }

    @Test
    fun drafts_selectionDeleteReentryAndDeleteAll_reloadDurableRepositoryState() {
        val firstDraft = timeLetter(id = 31L, title = "첫 임시 편지", status = TimeLetterStatus.DRAFT)
        val secondDraft = timeLetter(id = 32L, title = "둘째 임시 편지", status = TimeLetterStatus.DRAFT)
        val repository =
            FakeTimeLetterRepository(
                temporaryLetters = TimeLetterList(listOf(firstDraft, secondDraft), totalCount = 2),
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
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { repository.getTemporaryTimeLettersCalls == 2 }
        composeRule.onNodeWithText("첫 임시 편지").assertDoesNotExist()
        composeRule.onNodeWithText("둘째 임시 편지").assertIsDisplayed()

        composeRule.runOnIdle { activeViewModel.deleteAll() }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            val state = activeViewModel.uiState.value
            state is DraftLetterUiState.Success && state.drafts.isEmpty()
        }
        composeRule.onNodeWithText("둘째 임시 편지").assertDoesNotExist()
        assertEquals(1, repository.deleteAllTemporaryCalls)
        assertTrue(repository.temporaryLetters.timeLetters.isEmpty())
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
        val repository = FakeTimeLetterRepository(details = mapOf(existingLetter.id to existingLetter))
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
        assertEquals(listOf(41L), repository.requestedDetailIds)
        assertEquals(0, repository.getTimeLettersCalls)
        assertEquals(0, repository.createCalls.size)
        assertEquals(listOf(7L), viewModel.uiState.value.recipientIds)
    }

    @Test
    fun recipientListAndDetail_errorRetry_recoversBothRepositoryBoundaries() {
        val receivedLetter = receivedTimeLetter()
        val listResults =
            ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("list offline")),
                    Result.success(ReceivedTimeLetterList(listOf(receivedLetter), 1)),
                ),
            )
        val detailResults =
            ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("detail offline")),
                    Result.success(receivedLetter),
                ),
            )
        val repository =
            FakeReceiverTimeLetterRepository.strict().apply {
                onGetReceivedTimeLetters = { listResults.removeFirst().getOrThrow() }
                onGetReceivedTimeLetterDetail = { detailResults.removeFirst().getOrThrow() }
            }
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
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            listViewModel.uiState.value is RecipientTimeletterUiState.Success
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("받은 편지"))
        composeRule.onNodeWithText("받은 편지").assertIsDisplayed()
        assertEquals(2, repository.getReceivedTimeLettersCalls)

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
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            detailViewModel?.uiState?.value is RecipientTimeLetterDetailUiState.Success
        }
        composeRule.onNodeWithText("받은 편지").assertIsDisplayed()
        composeRule.onNodeWithText("복구된 상세 본문").assertIsDisplayed()
        assertEquals(listOf(71L, 71L), repository.requestedDetailIds)
    }

    private fun deleteVisibleLetter() {
        composeRule.onNodeWithContentDescription("더보기 설정").performClick()
        composeRule.onNodeWithText("삭제").performClick()
        composeRule.onNodeWithText("예").performClick()
    }

    private fun writeViewModel(
        repository: FakeTimeLetterRepository,
        userRepository: UserRepository,
        timeLetterId: Long,
    ): TimeLetterWriteViewModel {
        val resolver = ResolveTimeLetterBlocksUseCase(FakePhotoUploadRepository.strict())
        return TimeLetterWriteViewModel(
            createTimeLetterUseCase = CreateTimeLetterUseCase(repository, resolver),
            resolveTimeLetterBlocksUseCase = resolver,
            timeLetterRepository = repository,
            userRepository = userRepository,
            fileMetadataRepository = FakeFileMetadataRepository.strict(),
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
