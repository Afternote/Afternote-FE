package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.WeeklyReportResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeeklyReportApiService {
    @GET("mind-record")
    suspend fun getWeeklyReport(
        @Query("date") date: String,
    ): BaseResponse<WeeklyReportResponse>
}
