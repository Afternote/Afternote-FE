package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.DeepThoughtListDto
import retrofit2.http.GET
import retrofit2.http.Query

interface DeepThoughtApiService {
    @GET("deep-thought")
    suspend fun getDeepThoughts(
        @Query("draftOnly") draftOnly: Boolean? = null,
    ): BaseResponse<DeepThoughtListDto>
}
