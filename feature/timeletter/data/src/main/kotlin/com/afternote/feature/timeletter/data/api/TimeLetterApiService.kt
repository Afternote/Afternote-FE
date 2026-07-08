package com.afternote.feature.timeletter.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.timeletter.data.dto.TimeLetterCreateRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterDeleteRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterListResponseDto
import com.afternote.feature.timeletter.data.dto.TimeLetterResponseDto
import com.afternote.feature.timeletter.data.dto.TimeLetterUpdateRequest
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
    suspend fun getTimeLetters(): BaseResponse<TimeLetterListResponseDto>

    // 임시저장 전체 조회 (DRAFT)
    @GET("time-letters/temporary")
    suspend fun getTemporaryTimeLetters(): BaseResponse<TimeLetterListResponseDto>

    // 타임레터 단일 조회
    @GET("time-letters/{timeLetterId}")
    suspend fun getTimeLetter(
        @Path("timeLetterId") timeLetterId: Long,
    ): BaseResponse<TimeLetterResponseDto>

    // 타임레터 등록 (DRAFT or SCHEDULED)
    @POST("time-letters")
    suspend fun createTimeLetter(
        @Body request: TimeLetterCreateRequest,
    ): BaseResponse<TimeLetterResponseDto>

    // 타임레터 수정
    @PATCH("time-letters/{timeLetterId}")
    suspend fun updateTimeLetter(
        @Path("timeLetterId") timeLetterId: Long,
        @Body request: TimeLetterUpdateRequest,
    ): BaseResponse<TimeLetterResponseDto>

    // 타임레터 단일/다건 삭제 (body 를 가진 DELETE)
    @HTTP(method = "DELETE", path = "time-letters", hasBody = true)
    suspend fun deleteTimeLetters(
        @Body request: TimeLetterDeleteRequest,
    ): BaseResponse<Unit>

    // 임시저장 전체 삭제
    @DELETE("time-letters/temporary")
    suspend fun deleteAllTemporary(): BaseResponse<Unit>
}
