package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.MindRecordDetailResponse
import com.afternote.feature.mindrecord.data.dto.MindRecordListResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface MindRecordReceiverApiService {
    @GET("receiver-auth/mind-records")
    suspend fun getReceiverMindRecords(): BaseResponse<MindRecordListResponse>

    @GET("receiver-auth/mind-records/{mindRecordId}")
    suspend fun getReceiverMindRecordDetail(
        @Path("mindRecordId") mindRecordId: Long,
    ): BaseResponse<MindRecordDetailResponse>
}
