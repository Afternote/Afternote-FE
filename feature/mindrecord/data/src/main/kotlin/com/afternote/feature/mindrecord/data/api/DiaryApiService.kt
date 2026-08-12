package com.afternote.feature.mindrecord.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.dto.DiaryCreateRequestDto
import com.afternote.feature.mindrecord.data.dto.DiaryListDto
import com.afternote.feature.mindrecord.data.dto.DiaryUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DiaryApiService {
    @GET("diary")
    suspend fun getDiaries(
        @Query("yearMonth") yearMonth: String,
        @Query("draftOnly") draftOnly: Boolean? = null,
    ): BaseResponse<DiaryListDto>

    @POST("diary")
    suspend fun createDiary(
        @Body request: DiaryCreateRequestDto,
    ): BaseResponse<Unit>

    @PATCH("diary/{diaryId}")
    suspend fun updateDiary(
        @Path("diaryId") diaryId: Long,
        @Body request: DiaryUpdateRequestDto,
    ): BaseResponse<Unit>

    @DELETE("diary/{diaryId}")
    suspend fun deleteDiary(
        @Path("diaryId") diaryId: Long,
    ): BaseResponse<Unit>
}
