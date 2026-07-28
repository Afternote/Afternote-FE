package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.DailyQuestionCreateRequestDto
import com.afternote.feature.mindrecord.data.dto.DailyQuestionListItemDto
import com.afternote.feature.mindrecord.data.dto.DailyQuestionUpdateRequestDto
import com.afternote.feature.mindrecord.data.dto.TodayDailyQuestionDto
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
        // true 면 임시저장(isDraft=true)만 조회. 생략 시 서버가 임시저장을 제외한 답변만 내려준다.
        @Query("draftOnly") draftOnly: Boolean? = null,
    ): BaseResponse<List<DailyQuestionListItemDto>>

    @GET("daily-questions/today")
    suspend fun getTodayDailyQuestion(): BaseResponse<TodayDailyQuestionDto>

    @POST("daily-questions")
    suspend fun createDailyQuestion(
        @Body request: DailyQuestionCreateRequestDto,
    ): BaseResponse<Unit>

    @PATCH("daily-questions/{userDailyQuestionId}")
    suspend fun updateDailyQuestion(
        @Path("userDailyQuestionId") userDailyQuestionId: Long,
        @Body request: DailyQuestionUpdateRequestDto,
    ): BaseResponse<Unit>

    @DELETE("daily-questions/{userDailyQuestionId}")
    suspend fun deleteDailyQuestion(
        @Path("userDailyQuestionId") userDailyQuestionId: Long,
    ): BaseResponse<Unit>
}
