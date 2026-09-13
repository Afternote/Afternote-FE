package com.afternote.feature.home.presentation.usecase

import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.testing.FakeMindRecordReceiverRepository
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.testing.FakeReceiverTimeLetterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 수신자 홈 4-source 집계 정책 (#1689).
 *
 * 종전에는 이 판정이 `ReceiverHomeViewModel` 안에 있어 화면 상태를 통해서만 볼 수 있었다.
 * **전부 실패해야 전체 실패**이고 **무엇이 실패했는지**가 남아야 하는데, 그 둘은 UI 상태가
 * 아니라 집계의 결론이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetReceiverHomeSummaryUseCaseTest {
    @Test
    fun `네 출처가 모두 성공하면 값이 그대로 실린다`() =
        runTest {
            val result = useCase().invoke()

            val loaded = result as ReceiverHomeSummaryResult.Loaded
            assertNull("성공만 있는데 실패가 남았다", loaded.failure)
            assertEquals(6, loaded.summary.afternotes?.totalCount)
            assertEquals(4, loaded.summary.timeLetterTotalCount)
            assertEquals(
                2,
                loaded.summary.mindRecords
                    ?.dailyQuestions
                    ?.size,
            )
            assertEquals("박서연", loaded.summary.senderMessage?.senderName)
        }

    @Test
    fun `한 출처만 실패하면 나머지는 그대로 살고 실패한 이름이 남는다`() =
        runTest {
            val result =
                useCase(
                    receiver =
                        receiverRepository().apply {
                            onGetReceivedAfterNotes = { Result.failure(IOException("애프터노트 실패")) }
                        },
                ).invoke()

            val loaded = result as ReceiverHomeSummaryResult.Loaded
            assertEquals(listOf("afternotes"), loaded.failure?.failedSources)
            assertEquals("애프터노트 실패", loaded.failure?.firstCause?.message)
            assertNull("실패한 출처가 값을 남겼다", loaded.summary.afternotes)
            // 성공한 셋은 그대로 살아 있다 — 하나의 실패로 화면을 비우지 않는다.
            assertEquals(4, loaded.summary.timeLetterTotalCount)
            assertEquals(
                2,
                loaded.summary.mindRecords
                    ?.dailyQuestions
                    ?.size,
            )
            assertEquals("박서연", loaded.summary.senderMessage?.senderName)
        }

    @Test
    fun `출처마다 실패해도 나머지가 살아남는다`() =
        runTest {
            val failures =
                mapOf(
                    "afternotes" to {
                        r: FakeReceiverRepository,
                        m: FakeMindRecordReceiverRepository,
                        t: FakeReceiverTimeLetterRepository,
                        ->
                        r.onGetReceivedAfterNotes = { Result.failure(IOException("실패")) }
                    },
                    "mind_records" to {
                        _: FakeReceiverRepository,
                        m: FakeMindRecordReceiverRepository,
                        _: FakeReceiverTimeLetterRepository,
                        ->
                        m.onGetAll = { Result.failure(IOException("실패")) }
                    },
                    "time_letters" to {
                        _: FakeReceiverRepository,
                        _: FakeMindRecordReceiverRepository,
                        t: FakeReceiverTimeLetterRepository,
                        ->
                        t.onGetReceivedTimeLetters = { throw IOException("실패") }
                    },
                    "sender_message" to {
                        r: FakeReceiverRepository,
                        _: FakeMindRecordReceiverRepository,
                        _: FakeReceiverTimeLetterRepository,
                        ->
                        r.onLoadSenderMessage = { Result.failure(IOException("실패")) }
                    },
                )

            failures.forEach { (source, breakOne) ->
                val receiver = receiverRepository()
                val mindRecord = mindRecordRepository()
                val timeLetter = timeLetterRepository()
                breakOne(receiver, mindRecord, timeLetter)

                val loaded = useCase(receiver, mindRecord, timeLetter).invoke() as ReceiverHomeSummaryResult.Loaded

                assertEquals(source, listOf(source), loaded.failure?.failedSources)
            }
        }

    @Test
    fun `둘이 실패해도 전체 실패가 아니다`() =
        runTest {
            val result =
                useCase(
                    receiver =
                        receiverRepository().apply {
                            onGetReceivedAfterNotes = { Result.failure(IOException("애프터노트 실패")) }
                            onLoadSenderMessage = { Result.failure(IOException("메시지 실패")) }
                        },
                ).invoke()

            val loaded = result as ReceiverHomeSummaryResult.Loaded
            assertEquals(listOf("afternotes", "sender_message"), loaded.failure?.failedSources)
            assertEquals(4, loaded.summary.timeLetterTotalCount)
        }

    @Test
    fun `네 출처가 모두 실패해야 전체 실패다`() =
        runTest {
            val result =
                useCase(
                    receiver =
                        receiverRepository().apply {
                            onGetReceivedAfterNotes = { Result.failure(IOException("애프터노트 실패")) }
                            onLoadSenderMessage = { Result.failure(IOException("메시지 실패")) }
                        },
                    mindRecord = mindRecordRepository().apply { onGetAll = { Result.failure(IOException("기록 실패")) } },
                    timeLetter =
                        timeLetterRepository().apply {
                            onGetReceivedTimeLetters = { throw IOException("타임레터 실패") }
                        },
                ).invoke()

            val allFailed = result as ReceiverHomeSummaryResult.AllFailed
            assertEquals(
                listOf("afternotes", "mind_records", "time_letters", "sender_message"),
                allFailed.failure.failedSources,
            )
            // 첫 원인이 유실되지 않는다 — telemetry 가 예외 하나를 요구한다.
            assertEquals("애프터노트 실패", allFailed.failure.firstCause.message)
        }

    /**
     * 취소를 실패 폴백으로 바꾸지 않는다. 타임레터 저장소만 `Result` 가 아니라 값을 던져
     * `runCatchingCancellable` 로 감싸는데, 그 자리가 `CancellationException` 까지 삼키면
     * 화면을 떠난 뒤에도 「타임레터 조회 실패」 가 남는다.
     */
    @Test
    fun `취소되면 실패로 접히지 않고 구조화된 동시성이 함께 끊긴다`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val timeLetterCancelled = AtomicBoolean(false)
            val timeLetter =
                timeLetterRepository().apply {
                    onGetReceivedTimeLetters = {
                        started.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            timeLetterCancelled.set(true)
                        }
                    }
                }

            val job = async { useCase(timeLetter = timeLetter).invoke() }
            started.await()
            job.cancel()
            advanceUntilIdle()

            assertTrue("취소가 자식까지 내려가지 않았다", timeLetterCancelled.get())
            assertTrue("취소가 결과로 접혔다", job.isCancelled)
        }

    private fun useCase(
        receiver: FakeReceiverRepository = receiverRepository(),
        mindRecord: FakeMindRecordReceiverRepository = mindRecordRepository(),
        timeLetter: FakeReceiverTimeLetterRepository = timeLetterRepository(),
    ) = GetReceiverHomeSummaryUseCase(
        receiverRepository = receiver,
        mindRecordReceiverRepository = mindRecord,
        receiverTimeLetterRepository = timeLetter,
    )

    private fun receiverRepository() =
        FakeReceiverRepository.strict().apply {
            onGetReceivedAfterNotes = { Result.success(AfterNotesListResult(items = emptyList(), totalCount = 6)) }
            onLoadSenderMessage = {
                Result.success(
                    SenderMessageInfo(senderName = "박서연", message = "잘 지내렴", createdAt = "2026-09-01"),
                )
            }
        }

    private fun mindRecordRepository() =
        FakeMindRecordReceiverRepository().apply {
            onGetAll = {
                Result.success(
                    ReceiverMindRecords(
                        dailyQuestions = List(2) { index -> mindRecordSummary(index.toLong()) },
                        diaries = emptyList(),
                    ),
                )
            }
        }

    private fun mindRecordSummary(id: Long) =
        MindRecordSummary(
            id = id,
            type = MindRecordType.DAILY_QUESTION,
            title = "제목",
            content = "내용",
            recordDate = "2026-08-22",
            isDraft = false,
            createdAt = "2026.08.22 금",
        )

    private fun timeLetterRepository() =
        FakeReceiverTimeLetterRepository.strict().apply {
            onGetReceivedTimeLetters = { ReceivedTimeLetterList(timeLetters = emptyList(), totalCount = 4) }
        }
}
