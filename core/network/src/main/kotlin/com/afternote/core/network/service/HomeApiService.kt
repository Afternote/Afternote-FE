package com.afternote.core.network.service

import com.afternote.core.network.dto.HomeSummaryResponse
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.GET

interface HomeApiService {
    @GET("api/home/summary")
    suspend fun getHomeSummary(): BaseResponse<HomeSummaryResponse>
}
