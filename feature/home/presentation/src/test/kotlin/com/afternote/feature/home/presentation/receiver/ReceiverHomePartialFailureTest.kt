package com.afternote.feature.home.presentation.receiver

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.home.presentation.R
import com.afternote.feature.home.presentation.receiver.model.ReceiverHomeUiState
import com.afternote.feature.home.presentation.usecase.GetReceiverHomeSummaryUseCase
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.testing.FakeMindRecordReceiverRepository
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.testing.FakeReceiverTimeLetterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 이 테스트의 관심 밖인 외부 라우팅을 채우는 no-op 묶음. */
private val noopHomeActions =
    ReceiverHomeActions(
        onNavigateToMindRecord = {},
        onNavigateToTimeLetter = {},
        onNavigateToAfternote = {},
    )

/**
 * 수신자 홈의 **전체 실패 → 재시도 → 부분 성공** 을 화면까지 태워 확인한다 (#1689).
 *
 * 네 출처의 완료 시점을 테스트가 쥔다 — 그래야 「전부 실패해야 전체 실패」 와 「하나라도 성공하면
 * 그 출처로 그린다」 가 같은 실행 안에서 갈린다. 종전에는 app 계측 테스트였는데, 집계 계약이
 * 이 모듈 안으로 닫히면서(#1689) 조립도 계약 옆으로 왔다 — app 은 이 화면을 조립할 이유가 없다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ReceiverHomePartialFailureTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        // 네 조회가 `await()` 에 걸린 뒤 테스트가 완료 순서를 정하는 구조라, 디스패치 지연이 없는
        // 편이 판정이 선명하다 — 완료가 곧 재개다.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `전체 실패 뒤 재시도한 부분 성공은 살아남은 출처를 그리고 두 단계를 모두 기록한다`() {
        val afterNoteResults = ArrayDeque<CompletableDeferred<Result<AfterNotesListResult>>>()
        val senderMessageResults = ArrayDeque<CompletableDeferred<Result<SenderMessageInfo?>>>()
        val repository =
            FakeReceiverRepository.strict().apply {
                onGetReceivedAfterNotes = { afterNoteResults.removeFirst().await() }
                onLoadSenderMessage = { senderMessageResults.removeFirst().await() }
            }
        val mindRecordResults = ArrayDeque<CompletableDeferred<Result<ReceiverMindRecords>>>()
        val mindRecordRepository =
            FakeMindRecordReceiverRepository(onGetAll = { mindRecordResults.removeFirst().await() })
        val timeLetterResults = ArrayDeque<CompletableDeferred<Result<ReceivedTimeLetterList>>>()
        val timeLetterRepository =
            FakeReceiverTimeLetterRepository.strict().apply {
                onGetReceivedTimeLetters = { timeLetterResults.removeFirst().await().getOrThrow() }
            }

        fun homeCallCounts(): List<Int> =
            listOf(
                repository.getReceivedAfterNotesCalls,
                mindRecordRepository.getAllCalls,
                timeLetterRepository.getReceivedTimeLettersCalls,
                repository.loadSenderMessageCalls,
            )

        val allFailureAttempt = enqueueAttempt(afterNoteResults, mindRecordResults, timeLetterResults, senderMessageResults)
        val partialAttempt = enqueueAttempt(afterNoteResults, mindRecordResults, timeLetterResults, senderMessageResults)
        val reporter = PartialFailureErrorReporter()
        val viewModel =
            ReceiverHomeViewModel(
                getReceiverHomeSummary =
                    GetReceiverHomeSummaryUseCase(
                        receiverRepository = repository,
                        mindRecordReceiverRepository = mindRecordRepository,
                        receiverTimeLetterRepository = timeLetterRepository,
                    ),
                receiverRepository = repository,
                errorReporter = reporter,
            )

        composeRule.setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            AfternoteTheme {
                ReceiverHomeScreen(uiState = uiState, onEvent = viewModel::onEvent, actions = noopHomeActions)
            }
        }

        assertEquals(listOf(1, 1, 1, 1), homeCallCounts())
        assertSame(ReceiverHomeUiState.Loading, viewModel.uiState.value)

        val offline = IllegalStateException("offline")
        allFailureAttempt.complete(
            afterNotes = Result.failure(offline),
            mindRecords = Result.failure(offline),
            timeLetters = Result.failure(offline),
            senderMessage = Result.failure(offline),
        )
        composeRule.onNodeWithText(context.getString(R.string.home_receiver_error_message)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.home_receiver_retry)).performClick()

        assertEquals(listOf(2, 2, 2, 2), homeCallCounts())
        assertSame(ReceiverHomeUiState.Loading, viewModel.uiState.value)

        partialAttempt.complete(
            afterNotes =
                Result.success(
                    AfterNotesListResult(
                        items =
                            listOf(
                                AfterNoteListItem(1L, "Google Drive", AfternoteType.GALLERY_AND_FILES, null),
                                AfterNoteListItem(2L, "추억 노트", AfternoteType.MEMORIAL, null),
                            ),
                        totalCount = 2,
                    ),
                ),
            mindRecords = Result.failure(IllegalStateException("mind records unavailable")),
            timeLetters = Result.success(ReceivedTimeLetterList(timeLetters = emptyList(), totalCount = 8)),
            senderMessage =
                Result.success(
                    SenderMessageInfo(senderName = "이발신", message = "언제나 응원할게", createdAt = "2026.08.22"),
                ),
        )

        composeRule
            .onNodeWithText(context.getString(R.string.home_receiver_sender_record_title, "이발신"))
            .assertIsDisplayed()
        composeRule.onNodeWithText("언제나 응원할게").assertIsDisplayed()
        composeRule
            .onAllNodes(hasText(context.getString(R.string.home_receiver_section_count_unavailable)))
            .apply {
                assertCountEquals(2)
                this[0].performScrollTo().assertIsDisplayed()
            }
        composeRule.onNodeWithText("8개 라이프 이벤트 레터가 있습니다.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2개의 애프터노트가 있습니다.").performScrollTo().assertIsDisplayed()

        assertEquals(2, reporter.failures.size)
        assertEquals("receiver_home_load", reporter.failures[0].attributes["receiver_stage"])
        assertEquals("receiver_home_partial_load", reporter.failures[1].attributes["receiver_stage"])
        assertEquals("mind_records", reporter.failures[1].attributes["receiver_failed_sources"])
        assertEquals(listOf(2, 2, 2, 2), homeCallCounts())
    }
}

/** 홈 한 번의 로드가 물리는 네 대기열에 결과 게이트를 한 벌씩 건다. */
private fun enqueueAttempt(
    afterNoteResults: ArrayDeque<CompletableDeferred<Result<AfterNotesListResult>>>,
    mindRecordResults: ArrayDeque<CompletableDeferred<Result<ReceiverMindRecords>>>,
    timeLetterResults: ArrayDeque<CompletableDeferred<Result<ReceivedTimeLetterList>>>,
    senderMessageResults: ArrayDeque<CompletableDeferred<Result<SenderMessageInfo?>>>,
): PendingAttempt {
    val attempt =
        PendingAttempt(
            afterNotes = CompletableDeferred(),
            mindRecords = CompletableDeferred(),
            timeLetters = CompletableDeferred(),
            senderMessage = CompletableDeferred(),
        )
    afterNoteResults.addLast(attempt.afterNotes)
    mindRecordResults.addLast(attempt.mindRecords)
    timeLetterResults.addLast(attempt.timeLetters)
    senderMessageResults.addLast(attempt.senderMessage)
    return attempt
}

private class PendingAttempt(
    val afterNotes: CompletableDeferred<Result<AfterNotesListResult>>,
    val mindRecords: CompletableDeferred<Result<ReceiverMindRecords>>,
    val timeLetters: CompletableDeferred<Result<ReceivedTimeLetterList>>,
    val senderMessage: CompletableDeferred<Result<SenderMessageInfo?>>,
) {
    fun complete(
        afterNotes: Result<AfterNotesListResult>,
        mindRecords: Result<ReceiverMindRecords>,
        timeLetters: Result<ReceivedTimeLetterList>,
        senderMessage: Result<SenderMessageInfo?>,
    ) {
        this.afterNotes.complete(afterNotes)
        this.mindRecords.complete(mindRecords)
        this.timeLetters.complete(timeLetters)
        this.senderMessage.complete(senderMessage)
    }
}

private class PartialFailureErrorReporter : ErrorReporter {
    data class Failure(
        val throwable: Throwable,
        val attributes: Map<String, String>,
    )

    val failures = mutableListOf<Failure>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        failures += Failure(throwable, attributes)
    }
}
