package com.afternote.core.network.service

import com.afternote.core.network.dto.ReissueData
import com.afternote.core.network.dto.ReissueRequest
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenApiService {
    @POST("auth/reissue")
    suspend fun reissue(
        @Body body: ReissueRequest,
    ): BaseResponse<ReissueData>
}
