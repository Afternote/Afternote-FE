package com.afternote.feature.receiver.presentation.reporting

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiverFailureReportingTest {
    @Test
    fun `내보내기 미지원은 내려받기와 파일 저장 모두 오류로 보고하지 않는다`() {
        val reporter = RecordingErrorReporter()

        reporter.recordReceiverFailure(ReceiverFailureStage.RECEIVED_EXPORT_DOWNLOAD, ReceiverFailure.ExportNotSupported())
        reporter.recordReceiverFailure(ReceiverFailureStage.RECEIVED_EXPORT_SAVE, ReceiverFailure.ExportNotSupported())

        assertTrue(reporter.attributes.isEmpty())
    }

    @Test
    fun `일반 내보내기 실패는 각 단계와 오류 타입을 유지해 보고한다`() {
        val reporter = RecordingErrorReporter()
        val failure = IllegalStateException("export failed")

        reporter.recordReceiverFailure(ReceiverFailureStage.RECEIVED_EXPORT_DOWNLOAD, failure)
        reporter.recordReceiverFailure(ReceiverFailureStage.RECEIVED_EXPORT_SAVE, failure)

        assertEquals(
            listOf("received_export_download", "received_export_save"),
            reporter.attributes.map { it["receiver_stage"] },
        )
        assertTrue(reporter.attributes.all { it["error_type"] == failure.javaClass.name })
    }
}

private class RecordingErrorReporter : ErrorReporter {
    val attributes = mutableListOf<Map<String, String>>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        this.attributes += attributes
    }
}
