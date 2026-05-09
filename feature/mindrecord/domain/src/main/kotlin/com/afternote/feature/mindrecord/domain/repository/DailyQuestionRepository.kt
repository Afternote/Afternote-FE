package com.afternote.feature.mindrecord.domain.repository

import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion

interface DailyQuestionRepository {
    suspend fun getList(date: String? = null): Result<List<DailyQuestion>>

    suspend fun getToday(): Result<TodayDailyQuestion>

    suspend fun create(payload: DailyQuestionCreatePayload): Result<Unit>

    suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Unit>

    suspend fun delete(id: Long): Result<Unit>
}
