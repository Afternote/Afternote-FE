package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.ReceiverDailyQuestionListDto
import com.afternote.feature.mindrecord.data.dto.ReceiverDiaryListDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 수신자용 마음의 기록 조회 API 2종.
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
    ): BaseResponse<ReceiverDailyQuestionListDto>

    @GET("receiver-auth/diary")
    suspend fun getReceiverDiaries(
        @Query("sort") sort: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): BaseResponse<ReceiverDiaryListDto>
}
