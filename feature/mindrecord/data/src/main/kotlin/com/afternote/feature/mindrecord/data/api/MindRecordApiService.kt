package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.DailyQuestionResponse
import com.afternote.feature.mindrecord.data.dto.MindRecordResponse
import com.afternote.feature.mindrecord.data.model.MindRecordType
import com.afternote.feature.mindrecord.data.model.MindRecordViewType
import retrofit2.http.GET
import retrofit2.http.Query

interface MindRecordApiService {

    @GET("mind-records/list")
    suspend fun getMindRecords(
        @Query("type") type: MindRecordType,
        @Query("view") view: MindRecordViewType,
        @Query("year") year: Int?,
        @Query("month") month: Int?
    ): BaseResponse<MindRecordResponse>

    @GET("daily-question")
    suspend fun getDailyQuestion(): DailyQuestionResponse


}