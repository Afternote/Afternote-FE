package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.requireData
import com.afternote.feature.mindrecord.data.api.WeeklyReportApiService
import com.afternote.feature.mindrecord.data.mapper.toDomain
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import javax.inject.Inject

class WeeklyReportRepositoryImpl
    @Inject
    constructor(
        private val api: WeeklyReportApiService,
    ) : WeeklyReportRepository {
        override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> =
            runCatching {
                api.getWeeklyReport(date = date).requireData().toDomain()
            }
    }
