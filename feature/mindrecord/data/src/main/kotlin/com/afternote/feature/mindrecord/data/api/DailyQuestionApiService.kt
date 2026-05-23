package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.DailyQuestionCreateRequest
import com.afternote.feature.mindrecord.data.dto.DailyQuestionListItem
import com.afternote.feature.mindrecord.data.dto.DailyQuestionUpdateRequest
import com.afternote.feature.mindrecord.data.dto.TodayDailyQuestionResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DailyQuestionApiService {
    @GET("daily-questions")
    suspend fun getDailyQuestions(
        @Query("date") date: String? = null,
    ): BaseResponse<List<DailyQuestionListItem>>

    @GET("daily-questions/today")
    suspend fun getTodayDailyQuestion(): BaseResponse<TodayDailyQuestionResponse>

    @POST("daily-questions")
    suspend fun createDailyQuestion(
        @Body request: DailyQuestionCreateRequest,
    ): BaseResponse<Unit>

    @PATCH("daily-questions/{userDailyQuestionId}")
    suspend fun updateDailyQuestion(
        @Path("userDailyQuestionId") userDailyQuestionId: Long,
        @Body request: DailyQuestionUpdateRequest,
    ): BaseResponse<Unit>

    @DELETE("daily-questions/{userDailyQuestionId}")
    suspend fun deleteDailyQuestion(
        @Path("userDailyQuestionId") userDailyQuestionId: Long,
    ): BaseResponse<Unit>
}
