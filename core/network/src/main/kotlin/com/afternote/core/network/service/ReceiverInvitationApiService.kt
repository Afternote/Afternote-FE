package com.afternote.core.network.service

import com.afternote.core.network.dto.ReceiverInvitationAcceptDto
import com.afternote.core.network.dto.ReceiverInvitationCreateDto
import com.afternote.core.network.dto.ReceiverInvitationLookupDto
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 카카오톡 수신자 초대 API (#944).
 *
 * 실패 봉투 code — 1000(401 미인증) · 1905(404 초대 없음) · 1906(410 만료) · 1907(409 다른 사용자가
 * 이미 수락) · 1908(400 본인 초대 수락) · 1909(409 이미 그 초대자의 수신자). 번역은 core:data 가 한다.
 */
interface ReceiverInvitationApiService {
    /** 인증 필요, 바디 없음. */
    @POST("receiver-invitations")
    suspend fun createInvitation(): BaseResponse<ReceiverInvitationCreateDto>

    /** 비로그인 가능. 토큰은 경로 세그먼트라 Retrofit 이 인코딩한다(base64url 이라 실제로 바뀌는 문자는 없다). */
    @GET("receiver-invitations/{token}")
    suspend fun getInvitation(
        @Path("token") token: String,
    ): BaseResponse<ReceiverInvitationLookupDto>

    /** 인증 필요, 바디 없음. 같은 사용자의 재수락은 200 이다. */
    @POST("receiver-invitations/{token}/accept")
    suspend fun acceptInvitation(
        @Path("token") token: String,
    ): BaseResponse<ReceiverInvitationAcceptDto>
}
