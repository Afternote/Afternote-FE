package com.afternote.core.network.service

import com.afternote.core.network.dto.ReissueDto
import com.afternote.core.network.dto.ReissueRequestDto
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenApiService {
    @POST("auth/reissue")
    suspend fun reissue(
        @Body body: ReissueRequestDto,
    ): BaseResponse<ReissueDto>
}
