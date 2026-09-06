package com.afternote.feature.receiver.data.service

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.receiver.data.dto.DeliveryVerificationDto
import com.afternote.feature.receiver.data.dto.DeliveryVerificationRequestDto
import com.afternote.feature.receiver.data.dto.ReceivedRecordBoxListDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthCodeEmailSendRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthPresignedUrlDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthPresignedUrlRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthVerifyDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthVerifyRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverEmailAuthVerifyDto
import com.afternote.feature.receiver.data.dto.ReceiverEmailAuthVerifyRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverMessageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 수신자 인증 흐름 전용 API.
 *
 * 인증 코드 취득 전 단계인 `verify`·`email/auth-code`·`email/verify` 를 제외한 모든 endpoint 는
 * `X-Auth-Code` 헤더가 필요하며
 * [com.afternote.feature.receiver.data.network.ReceiverAuthInterceptor] 가
 * 저장된 인증 코드를 자동 부착한다 (저장된 코드가 없으면 헤더 없이 통과).
 */
interface ReceiverAuthApiService {
    @POST("receiver-auth/verify")
    suspend fun verifyMasterKey(
        @Body body: ReceiverAuthVerifyRequestDto,
    ): BaseResponse<ReceiverAuthVerifyDto>

    /** 수신자 레코드에 등록된 email 로 6자리 인증번호 발송. 미등록 email 은 404 (code 1901). */
    @POST("receiver-auth/email/auth-code")
    suspend fun sendEmailAuthCode(
        @Body body: ReceiverAuthCodeEmailSendRequestDto,
    ): BaseResponse<Unit>

    /** 발송된 6자리 인증번호 검증. 만료/미존재는 400 (code 1902), 불일치는 400 (code 1903). */
    @POST("receiver-auth/email/verify")
    suspend fun verifyEmailAuthCode(
        @Body body: ReceiverEmailAuthVerifyRequestDto,
    ): BaseResponse<ReceiverEmailAuthVerifyDto>

    @POST("receiver-auth/presigned-url")
    suspend fun getPresignedUrl(
        @Body body: ReceiverAuthPresignedUrlRequestDto,
    ): BaseResponse<ReceiverAuthPresignedUrlDto>

    @POST("receiver-auth/delivery-verification")
    suspend fun submitDeliveryVerification(
        @Body body: DeliveryVerificationRequestDto,
    ): BaseResponse<DeliveryVerificationDto>

    @GET("receiver-auth/delivery-verification/status")
    suspend fun getDeliveryVerificationStatus(): BaseResponse<DeliveryVerificationDto>

    @GET("receiver-auth/message")
    suspend fun getSenderMessage(): BaseResponse<ReceiverMessageDto>

    /**
     * 받은 기록함 목록. 서버가 `X-Auth-Code` 를 **이메일로 확장**해 같은 이메일에 등록된 기록함을
     * 전부 내려주므로, 응답에는 다른 발신자의 칸도 함께 들어 있다.
     */
    @GET("receiver-auth/record-boxes")
    suspend fun getReceivedRecordBoxes(): BaseResponse<ReceivedRecordBoxListDto>
}
