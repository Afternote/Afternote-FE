package com.afternote.feature.afternote.data.service

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.afternote.data.dto.DeliveryVerificationRequest
import com.afternote.feature.afternote.data.dto.DeliveryVerificationResponse
import com.afternote.feature.afternote.data.dto.ReceiverAuthPresignedUrlRequest
import com.afternote.feature.afternote.data.dto.ReceiverAuthPresignedUrlResponse
import com.afternote.feature.afternote.data.dto.ReceiverAuthVerifyRequest
import com.afternote.feature.afternote.data.dto.ReceiverAuthVerifyResponse
import com.afternote.feature.afternote.data.dto.ReceiverMessageResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 수신자 인증 흐름 전용 API.
 *
 * `verify` 외 모든 endpoint 는 `X-Auth-Code` 헤더가 필요하며
 * [com.afternote.feature.afternote.data.network.ReceiverAuthInterceptor] 가
 * 저장된 인증 코드를 자동 부착한다.
 */
interface ReceiverAuthApiService {
    @POST("receiver-auth/verify")
    suspend fun verify(
        @Body body: ReceiverAuthVerifyRequest,
    ): BaseResponse<ReceiverAuthVerifyResponse>

    @POST("receiver-auth/presigned-url")
    suspend fun getPresignedUrl(
        @Body body: ReceiverAuthPresignedUrlRequest,
    ): BaseResponse<ReceiverAuthPresignedUrlResponse>

    @POST("receiver-auth/delivery-verification")
    suspend fun submitDeliveryVerification(
        @Body body: DeliveryVerificationRequest,
    ): BaseResponse<DeliveryVerificationResponse>

    @GET("receiver-auth/delivery-verification/status")
    suspend fun getDeliveryVerificationStatus(): BaseResponse<DeliveryVerificationResponse>

    @GET("receiver-auth/message")
    suspend fun getSenderMessage(): BaseResponse<ReceiverMessageResponse>
}
