package com.afternote.feature.receiver.data.reporting

import com.afternote.core.common.reporting.ErrorReporter

/**
 * 기록된 실패를 들여다보는 테스트용 [ErrorReporter].
 *
 * `writeFailure` 를 구현한다 — `recordFailure` 는 인터페이스가 취소 제외·원문 redact 를
 * 처리한 뒤 이 함수를 부르므로, 그 정책까지 함께 태워야 실제 동작과 같아진다.
 */
internal class RecordingErrorReporter : ErrorReporter {
    data class Failure(
        val throwable: Throwable,
        val attributes: Map<String, String>,
    )

    val failures = mutableListOf<Failure>()

    val stages: List<String>
        get() = failures.mapNotNull { it.attributes["receiver_stage"] }

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        failures += Failure(throwable, attributes)
    }
}
