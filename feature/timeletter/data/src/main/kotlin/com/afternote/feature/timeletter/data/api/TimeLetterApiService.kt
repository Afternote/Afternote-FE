package com.afternote.feature.timeletter.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.timeletter.data.dto.TimeLetterCreateRequestDto
import com.afternote.feature.timeletter.data.dto.TimeLetterDeleteRequestDto
import com.afternote.feature.timeletter.data.dto.TimeLetterDto
import com.afternote.feature.timeletter.data.dto.TimeLetterListDto
import com.afternote.feature.timeletter.data.dto.TimeLetterUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface TimeLetterApiService {
    // 타임레터 전체 조회 (SCHEDULED)
    @GET("time-letters")
    suspend fun getTimeLetters(): BaseResponse<TimeLetterListDto>

    // 임시저장 전체 조회 (DRAFT)
    @GET("time-letters/temporary")
    suspend fun getTemporaryTimeLetters(): BaseResponse<TimeLetterListDto>

    // 타임레터 단일 조회
    @GET("time-letters/{timeLetterId}")
    suspend fun getTimeLetter(
        @Path("timeLetterId") timeLetterId: Long,
    ): BaseResponse<TimeLetterDto>

    // 타임레터 등록 (DRAFT or SCHEDULED)
    @POST("time-letters")
    suspend fun createTimeLetter(
        @Body request: TimeLetterCreateRequestDto,
    ): BaseResponse<TimeLetterDto>

    // 타임레터 수정
    @PATCH("time-letters/{timeLetterId}")
    suspend fun updateTimeLetter(
        @Path("timeLetterId") timeLetterId: Long,
        @Body request: TimeLetterUpdateRequestDto,
    ): BaseResponse<TimeLetterDto>

    // 타임레터 단일/다건 삭제
    @HTTP(method = "DELETE", path = "time-letters", hasBody = true)
    suspend fun deleteTimeLetters(
        @Body request: TimeLetterDeleteRequestDto,
    ): BaseResponse<Unit>

    // 임시저장 전체 삭제
    @DELETE("time-letters/temporary")
    suspend fun deleteAllTemporary(): BaseResponse<Unit>
}
