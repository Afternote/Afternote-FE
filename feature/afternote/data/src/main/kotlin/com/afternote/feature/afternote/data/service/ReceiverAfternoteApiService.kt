package com.afternote.feature.afternote.data.service

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.afternote.data.dto.ReceiverAfternoteListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ReceiverAfternoteApiService {
    @GET("receiver-auth/afternotes")
    suspend fun getReceiverAfternotes(
        @Query("category") category: String?,
        @Query("page") pageNumber: Int?,
        @Query("size") size: Int?,
    ): BaseResponse<ReceiverAfternoteListResponse>
}
