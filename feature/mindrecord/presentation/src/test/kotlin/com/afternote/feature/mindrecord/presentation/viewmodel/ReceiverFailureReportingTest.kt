package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.error.DeliveryNotReadyException
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * 수신자 열람 실패 계측이 «정상 상태» 까지 올리지 않는지 (#964 리뷰).
 *
 * 전달 조건 미충족(`DeliveryNotReadyException`)은 장애가 아니다 — 서버도 기기도 멀쩡하고
 * 발신자가 조건을 정해야 풀린다. 전달을 기다리는 수신자는 앱을 열 때마다 이 경로를 타므로,
 * 기록하면 Crashlytics 보관 한도(최근 8건)를 그 잡음이 채워 **정작 잡아야 할 열람 실패를
 * 밀어낸다** — 이 stage 를 승격한 이유 자체가 무력해진다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverFailureReportingTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `전달 조건 미충족은 기록하지 않는다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            ReceiverMindRecordViewModel(FailingReceiverRepository(DeliveryNotReadyException(null)), reporter)
            advanceUntilIdle()

            assertEquals(emptyList<String>(), reporter.stages)
        }

    @Test
    fun `그 밖의 열람 실패는 기록한다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            ReceiverMindRecordViewModel(FailingReceiverRepository(IOException("offline")), reporter)
            advanceUntilIdle()

            assertEquals(listOf("receiver_record_load"), reporter.stages)
        }
}

private class FailingReceiverRepository(
    private val failure: Throwable,
) : MindRecordReceiverRepository {
    override suspend fun getAll(): Result<ReceiverMindRecords> = Result.failure(failure)
}
