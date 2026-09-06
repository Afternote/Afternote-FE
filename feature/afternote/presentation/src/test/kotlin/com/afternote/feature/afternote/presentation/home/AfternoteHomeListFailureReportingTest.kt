package com.afternote.feature.afternote.presentation.home

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * 목록 로드 실패가 텔레메트리에 남는지, 그리고 재시도 연타가 보관함을 밀어내지 않는지 (#705).
 *
 * Paging 은 실패를 `LoadState.Error` 로만 알리고 삼킨다 — 화면이 넘겨 주지 않으면 [AfternotePagingSource]
 * 의 실패가 콘솔에 흔적을 남기지 않는다. 반대로 실패마다 그대로 기록하면 «다시 시도» 연타 한 번에
 * Crashlytics non-fatal 보관 한도(최근 8건)가 같은 실패로 채워져 실제 장애가 밀려난다.
 *
 * [ErrorReporter] 는 예외를 redact 하므로 단언은 `afternote_stage` · `error_type` 속성으로 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AfternoteHomeListFailureReportingTest {
    /** `viewModelScope` 가 Main 디스패처를 요구한다 — 계측 자체는 코루틴을 쓰지 않는다. */
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `목록 실패는 list_load 단계로 기록한다`() {
        val reporter = RecordingErrorReporter()
        val viewModel = viewModel(reporter)

        viewModel.onListLoadFailed(IOException("목록 조회 실패"))

        assertEquals(listOf("list_load"), reporter.recordedStages)
        assertEquals(listOf(IOException::class.java.name), reporter.recordedErrorTypes)
    }

    @Test
    fun `같은 장애가 이어지는 동안 재시도를 반복해도 한 번만 기록한다`() {
        val reporter = RecordingErrorReporter()
        val viewModel = viewModel(reporter)

        repeat(5) { viewModel.onListLoadFailed(IOException("목록 조회 실패")) }

        assertEquals(1, reporter.recordedStages.size)
    }

    @Test
    fun `장애 종류가 바뀌면 새 사건으로 기록한다`() {
        val reporter = RecordingErrorReporter()
        val viewModel = viewModel(reporter)

        viewModel.onListLoadFailed(IOException("연결 실패"))
        viewModel.onListLoadFailed(SocketTimeoutException("응답 없음"))

        assertEquals(
            listOf(IOException::class.java.name, SocketTimeoutException::class.java.name),
            reporter.recordedErrorTypes,
        )
    }

    @Test
    fun `성공한 로드가 실패 구간을 닫아 다음 실패를 다시 기록한다`() {
        val reporter = RecordingErrorReporter()
        val viewModel = viewModel(reporter)

        viewModel.onListLoadFailed(IOException("목록 조회 실패"))
        viewModel.onListLoadSucceeded()
        viewModel.onListLoadFailed(IOException("목록 조회 실패"))

        assertEquals(listOf("list_load", "list_load"), reporter.recordedStages)
    }

    private fun viewModel(reporter: ErrorReporter): AfternoteHomeViewModel =
        AfternoteHomeViewModel(
            afternoteRepository = FakeAfternoteRepository(),
            errorReporter = reporter,
        )

    private class RecordingErrorReporter : ErrorReporter {
        val recordedStages = mutableListOf<String>()
        val recordedErrorTypes = mutableListOf<String>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            attributes["afternote_stage"]?.let(recordedStages::add)
            attributes["error_type"]?.let(recordedErrorTypes::add)
        }
    }
}
