package com.afternote.feature.afternote.data.service

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.afternote.data.dto.ReceivedAfternoteDetailResponse
import com.afternote.feature.afternote.data.dto.ReceivedAfternoteListResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ReceiverAfternoteApiService {
    /**
     * 인증번호로 수신한 애프터노트 목록 조회. 서버는 페이지네이션 없이 전체 목록과 totalCount만 반환한다.
     * `X-Auth-Code` 헤더는 [com.afternote.feature.afternote.data.network.ReceiverAuthInterceptor]가 자동 부착한다.
     */
    @GET("api/v1/receiver-auth/after-notes")
    suspend fun getReceiverAfternotes(): BaseResponse<ReceivedAfternoteListResponse>

    /** 인증번호로 수신한 특정 애프터노트의 상세 조회. */
    @GET("api/v1/receiver-auth/after-notes/{afternoteId}")
    suspend fun getReceiverAfternoteDetail(
        @Path("afternoteId") afternoteId: Long,
    ): BaseResponse<ReceivedAfternoteDetailResponse>
}
