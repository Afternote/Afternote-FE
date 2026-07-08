package com.afternote.core.network.service

import com.afternote.core.network.dto.DeliveryConditionRequest
import com.afternote.core.network.dto.DeliveryConditionResponseDto
import com.afternote.core.network.dto.ReceiverDeliveryConditionResponseDto
import com.afternote.core.network.dto.ReceiverDeliveryConditionUpdateRequest
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
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

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

    // [DEPRECATED] 유저 단위 전달 조건 — 스웨거에서 제거됨. 수신자별 delivery-conditions 로 대체 예정.
    // 사후 관리 재설계(수신자 × 콘텐츠) 마이그레이션 시 이 두 메서드와 관련 화면을 함께 정리해야 한다.
    // 전달 조건 조회
    @GET("users/delivery-condition")
    suspend fun getDeliveryCondition(): BaseResponse<DeliveryConditionResponseDto>

    // 전달 조건 수정
    @PATCH("users/delivery-condition")
    suspend fun updateDeliveryCondition(
        @Body request: DeliveryConditionRequest,
    ): BaseResponse<DeliveryConditionResponseDto>

    // 활동 기록(ping) — 앱 실행 시 호출해 미사용 타이머 리셋
    @POST("users/me/activity")
    suspend fun recordActivity(): BaseResponse<Unit>

    // 수신자별 전달 조건 조회 (사후 관리)
    @GET("users/me/receivers/{receiverId}/delivery-conditions")
    suspend fun getReceiverDeliveryConditions(
        @Path("receiverId") receiverId: Long,
    ): BaseResponse<ReceiverDeliveryConditionResponseDto>

    // 수신자별 전달 조건 설정/변경 (사후 관리)
    @PUT("users/me/receivers/{receiverId}/delivery-conditions")
    suspend fun updateReceiverDeliveryConditions(
        @Path("receiverId") receiverId: Long,
        @Body request: ReceiverDeliveryConditionUpdateRequest,
    ): BaseResponse<ReceiverDeliveryConditionResponseDto>
}
