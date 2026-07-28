package com.afternote.feature.mindrecord.domain.repository

import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion

interface DailyQuestionRepository {
    /**
     * 답변 목록 조회.
     *
     * @param date 조회할 날짜 (yyyy-MM-dd). null 이면 전체 기간.
     * @param draftOnly true 면 임시저장만 조회. null(생략) 이면 서버가 임시저장을 제외한 답변만 내려준다.
     */
    suspend fun getList(
        date: String? = null,
        draftOnly: Boolean? = null,
    ): Result<List<DailyQuestion>>

    suspend fun getToday(): Result<TodayDailyQuestion>

    suspend fun create(payload: DailyQuestionCreatePayload): Result<Unit>

    suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Unit>

    suspend fun delete(id: Long): Result<Unit>
}
