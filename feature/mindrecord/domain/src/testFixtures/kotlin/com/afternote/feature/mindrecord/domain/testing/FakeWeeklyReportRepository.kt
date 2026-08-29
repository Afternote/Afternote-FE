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
class FakeWeeklyReportRepository(
    /**
     * 큐가 비었을 때 돌려줄 값. `null` 이면 **터뜨린다**(기본).
     *
     * 기본이 터뜨리는 쪽인 이유는 위와 같다 — 조용히 빈 리포트를 돌려주면 요청 횟수가 어긋난 것을
     * 놓친다. 다만 **이 저장소를 보지 않는 테스트까지 같이 죽는 자리**가 있다: 홈이 진입 시
     * 주간 기록 수를 부르게 되면서(#562) 로그인·홈 계측 전부가 이 경로를 지난다. 그 테스트들은
     * 주간 수에 관심이 없고, 화면도 실패를 0 이 아니라 미상(대시)으로 그리므로 실패를 돌려주면
     * 나머지 단언을 방해하지 않는다. 그런 배선은 이 인자로 «큐가 비면 실패» 를 명시한다.
     */
    private val whenQueueEmpty: Result<WeeklyReport>? = null,
) : WeeklyReportRepository {
    val results = ArrayDeque<Result<WeeklyReport>>()
    val requestedDates = mutableListOf<String>()

    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
        requestedDates += date
        return results.removeFirstOrNull()
            ?: whenQueueEmpty
            ?: error("$date 주간 리포트 응답이 큐에 없다")
    }
}
