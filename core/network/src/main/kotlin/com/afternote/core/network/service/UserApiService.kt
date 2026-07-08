package com.afternote.core.network.service

import com.afternote.core.network.dto.DeliveryConditionRequest
import com.afternote.core.network.dto.DeliveryConditionResponseDto
import com.afternote.core.network.dto.ReceiverDetailResponseDto
import com.afternote.core.network.dto.ReceiverListResponseDto
import com.afternote.core.network.dto.SocialAccountLinkRequest
import com.afternote.core.network.dto.UserConnectedAccountResponseDto
import com.afternote.core.network.dto.UserCreateReceiverRequest
import com.afternote.core.network.dto.UserCreateReceiverResponseDto
import com.afternote.core.network.dto.UserPatchReceiverRequest
import com.afternote.core.network.dto.UserPatchReceiverResponseDto
import com.afternote.core.network.dto.UserPushSettingResponseDto
import com.afternote.core.network.dto.UserResponseDto
import com.afternote.core.network.dto.UserUpdateProfileRequest
import com.afternote.core.network.dto.UserUpdatePushSettingRequest
import com.afternote.core.network.dto.UserUpdateReceiverMessageRequest
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionResponse
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionUpdateRequest
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApiService {
    // 수신자 목록 조회
    @GET("users/receivers")
    suspend fun getReceivers(): BaseResponse<List<ReceiverListResponseDto>>

    // 수신자 등록
    @POST("users/receivers")
    suspend fun createReceiver(
        @Body request: UserCreateReceiverRequest,
    ): BaseResponse<UserCreateReceiverResponseDto>

    // 수신자 상세 조회
    @GET("users/receivers/{receiverId}")
    suspend fun getReceiverDetail(
        @Path("receiverId") receiverId: Long,
    ): BaseResponse<ReceiverDetailResponseDto>

    // 수신자 정보 수정
    @PATCH("users/receivers/{receiverId}")
    suspend fun updateReceiver(
        @Path("receiverId") receiverId: Long,
        @Body request: UserPatchReceiverRequest,
    ): BaseResponse<UserPatchReceiverResponseDto>

    // 수신자 메시지 수정
    @PATCH("users/receivers/{receiverId}/message")
    suspend fun updateReceiverMessage(
        @Path("receiverId") receiverId: Long,
        @Body request: UserUpdateReceiverMessageRequest,
    ): BaseResponse<Unit>

    // 내 프로필 조회
    @GET("users/me")
    suspend fun getMyProfile(): BaseResponse<UserResponseDto>

    // 프로필 수정
    @PATCH("users/me")
    suspend fun updateMyProfile(
        @Body request: UserUpdateProfileRequest,
    ): BaseResponse<UserResponseDto>

    // 회원 탈퇴
    @DELETE("users/me")
    suspend fun deleteAccount(): BaseResponse<Unit>

    // 푸시 알림 설정 조회
    @GET("users/push-settings")
    suspend fun getMyPushSettings(): BaseResponse<UserPushSettingResponseDto>

    // 푸시 알림 설정 수정
    @PATCH("users/push-settings")
    suspend fun updateMyPushSettings(
        @Body request: UserUpdatePushSettingRequest,
    ): BaseResponse<UserPushSettingResponseDto>

    // 연결된 계정 조회
    @GET("users/connected-accounts")
    suspend fun getConnectedAccounts(): BaseResponse<UserConnectedAccountResponseDto>

    // 소셜 계정 연결
    @POST("users/connected-accounts/{provider}")
    suspend fun linkConnectedAccount(
        @Path("provider") provider: String,
        @Body request: SocialAccountLinkRequest,
    ): BaseResponse<UserConnectedAccountResponseDto>

    // 소셜 계정 연결 해제
    @DELETE("users/connected-accounts/{provider}")
    suspend fun unlinkConnectedAccount(
        @Path("provider") provider: String,
    ): BaseResponse<UserConnectedAccountResponseDto>

    // 전달 조건 조회
    @GET("users/delivery-condition")
    suspend fun getDeliveryCondition(): BaseResponse<DeliveryConditionResponseDto>

    // 전달 조건 수정
    @PATCH("users/delivery-condition")
    suspend fun updateDeliveryCondition(
        @Body request: DeliveryConditionRequest,
    ): BaseResponse<DeliveryConditionResponseDto>

    /**
     * 수신자별 전달조건 조회 — 특정 수신자(receiverId)에게 **콘텐츠 종류마다** 다르게 건 전달 조건 목록.
     *
     * 사후 전달을 (수신자 × 콘텐츠) 단위로 쪼갠 재설계의 핵심 축 (이슈 #427). 유저 전체에 조건 1개였던
     * 구조를 대체한다. 예: 엄마에겐 애프터노트를 "1년 미사용 시"(INACTIVITY), 다이어리를 "본인이 사망증빙
     * 후 요청 시"(RECEIVER_REQUEST) 로 각각 걸 수 있다. 조건 1건 = contentType + conditionType +
     * inactivityPeriod(INACTIVITY 한정) + state(진행 상태).
     */
    @GET("users/me/receivers/{receiverId}/delivery-conditions")
    suspend fun getReceiverDeliveryConditions(
        @Path("receiverId") receiverId: Long,
    ): BaseResponse<ReceiverDeliveryConditionResponse>

    /**
     * 수신자별 전달조건 설정/변경 — 보낸 conditions[] 로 저장하고, **변경이 반영된 최신 전체 목록**을
     * [getReceiverDeliveryConditions] 와 동일한 구조로 응답한다.
     *
     * **응답을 받아 쓰는 이유**: 요청 바디엔 조건 3필드(contentType·conditionType·inactivityPeriod)만
     * 담기지만, 화면에 필요한 state·fulfilled·gracePeriodStartedAt 은 서버만 계산할 수 있는 판정값이다
     * (기존 서류/검증 DB 를 봐야 승인 대기인지 이미 충족인지 판단). 응답이 이 값들을 채워 주므로 그대로
     * 화면에 반영하면 되고, 저장 후 상태 갱신용 GET 재조회가 불필요하다(왕복 1회 절약 + 로컬/서버 즉시 일치).
     *
     * 서버 규칙(Swagger): INACTIVITY 는 inactivityPeriod(3/6/12개월) 필수, RECEIVER_REQUEST 는 서류
     * 제출 후 운영자 승인. **조건을 바꿔도 이미 제출된 서류/검증 상태는 유지**되어 응답의 state 에 반영된다.
     */
    @PUT("users/me/receivers/{receiverId}/delivery-conditions")
    suspend fun updateReceiverDeliveryConditions(
        @Path("receiverId") receiverId: Long,
        @Body request: ReceiverDeliveryConditionUpdateRequest,
    ): BaseResponse<ReceiverDeliveryConditionResponse>
}
