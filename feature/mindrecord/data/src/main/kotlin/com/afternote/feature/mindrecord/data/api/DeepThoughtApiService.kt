package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.DeepThoughtCreateRequest
import com.afternote.feature.mindrecord.data.dto.DeepThoughtListItem
import com.afternote.feature.mindrecord.data.dto.DeepThoughtUpdateRequest
import com.afternote.feature.mindrecord.data.dto.RandomDeepThoughtResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DeepThoughtApiService {
    @GET("deep-thought")
    suspend fun getDeepThoughts(
        @Query("date") date: String? = null,
        @Query("tag") tag: String? = null,
        @Query("category") category: String? = null,
    ): BaseResponse<List<DeepThoughtListItem>>

    @GET("deep-thought/random")
    suspend fun getRandomDeepThought(): BaseResponse<RandomDeepThoughtResponse>

    @POST("deep-thought")
    suspend fun createDeepThought(
        @Body request: DeepThoughtCreateRequest,
    ): BaseResponse<Unit>

    @PATCH("deep-thought/{deepThoughtId}")
    suspend fun updateDeepThought(
        @Path("deepThoughtId") deepThoughtId: Long,
        @Body request: DeepThoughtUpdateRequest,
    ): BaseResponse<Unit>

    @DELETE("deep-thought/{deepThoughtId}")
    suspend fun deleteDeepThought(
        @Path("deepThoughtId") deepThoughtId: Long,
    ): BaseResponse<Unit>
}
