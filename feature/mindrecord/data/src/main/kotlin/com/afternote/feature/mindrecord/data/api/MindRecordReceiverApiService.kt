package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.ReceiverDailyQuestionListResponse
import com.afternote.feature.mindrecord.data.dto.ReceiverDeepThoughtListResponse
import com.afternote.feature.mindrecord.data.dto.ReceiverDiaryListResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 수신자용 마음의 기록 조회 API 3종.
 *
 * `X-Auth-Code` 헤더는 `/receiver-auth/` 경로 기준으로
 * [com.afternote.feature.afternote.data.network.ReceiverAuthInterceptor]가 자동 부착한다.
 */
interface MindRecordReceiverApiService {
    @GET("receiver-auth/daily-question")
    suspend fun getReceiverDailyQuestions(
        @Query("sort") sort: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): BaseResponse<ReceiverDailyQuestionListResponse>

    @GET("receiver-auth/diary")
    suspend fun getReceiverDiaries(
        @Query("sort") sort: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): BaseResponse<ReceiverDiaryListResponse>

    @GET("receiver-auth/deep-thought")
    suspend fun getReceiverDeepThoughts(
        @Query("category") category: String? = null,
        @Query("tag") tag: String? = null,
        @Query("sort") sort: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): BaseResponse<ReceiverDeepThoughtListResponse>
}
