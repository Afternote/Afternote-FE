package com.afternote.feature.mindrecord.presentation.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * 마음의 기록 계측 규격 (#964).
 *
 * 이 모듈은 종전에 `Log.e` 도 `ErrorReporter` 도 0건이라, 실패가 UI 상태로만 흡수되고
 * 릴리즈에서는 아무 흔적도 남지 않았다.
 */
class MindRecordFailureReportingTest {
    private val reporter = RecordingErrorReporter()

    @Test
    fun `단계는 전용 키로 기록된다`() {
        // 키를 다른 흐름과 합치면 콘솔에서 한 키에 서로 다른 값 목록이 섞인다.
        reporter.recordMindRecordFailure(MindRecordFailureStage.DRAFT_LIST_LOAD, IOException("offline"))

        val recorded = reporter.failures.single()
        assertEquals("draft_list_load", recorded.attributes["mind_record_stage"])
    }

    @Test
    fun `예외 원문은 콘솔로 나가지 않는다`() {
        // 서버 응답 본문이 message 에 그대로 담겨 오는 경우가 있어 창구가 redact 한다.
        reporter.recordMindRecordFailure(
            MindRecordFailureStage.RECEIVER_RECORD_LOAD,
            IllegalStateException("user@example.com 의 열람 권한이 없습니다"),
        )

        val recorded = reporter.failures.single()
        assertTrue(
            "예외 원문이 그대로 실렸다: ${recorded.throwable.message}",
            recorded.throwable.message?.contains("user@example.com") != true,
        )
        // 대신 타입은 남아야 어떤 실패인지 가릴 수 있다.
        assertEquals(
            "java.lang.IllegalStateException",
            recorded.attributes.values.firstOrNull { it.startsWith("java.lang.IllegalStateException") },
        )
    }

    @Test
    fun `코루틴 취소는 기록하지 않는다`() {
        // 「취소는 에러가 아니다」를 창구에서 지킨다. 화면 이탈이 잦은 목록에서 취소가 전부
        // 기록되면 보관 한도(최근 8건)를 잡음이 채운다 — 호출부가 하나라도 빠뜨리면 그렇게 된다.
        reporter.recordMindRecordFailure(MindRecordFailureStage.DRAFT_LIST_LOAD, CancellationException("화면 이탈"))

        assertTrue("취소가 기록됐다", reporter.failures.isEmpty())
    }

    @Test
    fun `단계 이름은 서로 겹치지 않는다`() {
        // reportingName 은 콘솔 필터 값이라 겹치면 다른 화면의 실패가 한 그룹으로 묶인다.
        val names = MindRecordFailureStage.entries.map { it.reportingName }

        assertEquals(names.size, names.toSet().size)
    }
}
