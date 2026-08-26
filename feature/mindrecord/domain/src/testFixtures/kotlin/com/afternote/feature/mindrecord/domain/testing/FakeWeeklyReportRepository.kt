package com.afternote.feature.mindrecord.domain.testing

import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository

/**
 * [WeeklyReportRepository] fake 정본 (#1030).
 *
 * 이 화면의 테스트는 "요청한 주" 와 "응답 순서" 를 함께 본다 — 응답을 [results] 로
 * 밀어 넣고 [requestedDates] 로 무엇을 요청했는지 확인한다. 큐가 비면 실패시킨다:
 * 조용히 빈 리포트를 돌려주면 요청 횟수가 어긋난 것을 놓친다.
 */
class FakeWeeklyReportRepository : WeeklyReportRepository {
    val results = ArrayDeque<Result<WeeklyReport>>()
    val requestedDates = mutableListOf<String>()

    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
        requestedDates += date
        return requireNotNull(results.removeFirstOrNull()) { "$date 주간 리포트 응답이 큐에 없다" }
    }
}
