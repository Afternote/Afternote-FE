package com.afternote.feature.mindrecord.domain.testing

import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository

/**
 * [WeeklyReportRepository] fake 정본 (#1030).
 *
 * 이 화면의 테스트는 "요청한 주" 와 "응답 순서" 를 함께 본다 — 응답을 [results] 로
 * 밀어 넣고 [requestedDates] 로 무엇을 요청했는지 확인한다. 큐가 비면 실패시킨다:
 * 조용히 빈 리포트를 돌려주면 요청 횟수가 어긋난 것을 놓친다.
 *
 * @param fallback 큐가 비었을 때 돌려줄 응답. **주간 리포트를 보지 않는 테스트를 위한 자리**다.
 *   앱 전체를 띄우는 계측은 홈을 지나가기만 해도 이 저장소를 부르고, 그 횟수는 화면 전환·
 *   `ON_RESUME` 에 따라 달라져 미리 셀 수 없다. 그때 큐를 비워 두고 터뜨리면 **홈이 못 뜨고,
 *   실패는 「인사말이 안 보인다」 같은 엉뚱한 자리에서 나타난다** (#562 · #1288).
 *
 *   기본값 `null` 이라 **종전 동작(큐가 비면 실패)이 그대로다** — 요청 횟수를 세는 화면 테스트는
 *   손댈 필요가 없다. `fallback` 을 준 자리에서도 [requestedDates] 는 계속 쌓이므로 세는 단언을
 *   잃지 않는다.
 */
class FakeWeeklyReportRepository(
    private val fallback: Result<WeeklyReport>? = null,
) : WeeklyReportRepository {
    val results = ArrayDeque<Result<WeeklyReport>>()
    val requestedDates = mutableListOf<String>()

    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
        requestedDates += date
        results.removeFirstOrNull()?.let { return it }
        return requireNotNull(fallback) { "$date 주간 리포트 응답이 큐에 없다" }
    }
}
