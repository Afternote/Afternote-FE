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

    /**
     * 답변 생성.
     *
     * 서버가 돌려주는 `userDailyQuestionId`("내 답변" 식별자)를 그대로 올린다 — 저장 직후
     * 그 레코드를 가리켜야 하는 흐름(이어쓰기·수정·삭제)이 목록을 다시 뒤지지 않게 한다 (#573).
     */
    suspend fun create(payload: DailyQuestionCreatePayload): Result<Long>

    /** 답변 수정. 생성과 같은 스키마로 응답하므로 식별자를 그대로 올린다 (#573). */
    suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long>

    suspend fun delete(id: Long): Result<Unit>
}
