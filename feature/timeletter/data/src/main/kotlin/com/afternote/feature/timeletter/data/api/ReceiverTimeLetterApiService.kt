package com.afternote.feature.timeletter.data.api

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.timeletter.data.dto.ReceivedTimeLetterListResponseDto
import com.afternote.feature.timeletter.data.dto.ReceivedTimeLetterResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ReceiverTimeLetterApiService {
    @GET("receiver-auth/time-letters")
    suspend fun getReceivedTimeLetters(): BaseResponse<ReceivedTimeLetterListResponseDto>

    @GET("receiver-auth/time-letters/{timeLetterReceiverId}")
    suspend fun getReceivedTimeLetterDetail(
        @Path("timeLetterReceiverId") timeLetterReceiverId: Long,
    ): BaseResponse<ReceivedTimeLetterResponseDto>
}
