package com.afternote.feature.mindrecord.domain.repository

import com.afternote.feature.mindrecord.domain.model.WeeklyReport

interface WeeklyReportRepository {
    /**
     * @param date 조회하려는 주의 **월요일** 날짜 (`yyyy-MM-dd`).
     */
    suspend fun getWeeklyReport(date: String): Result<WeeklyReport>
}
