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
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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

    /**
     * 활동 기록(ping) — 서버에 "이 사용자가 방금 활동했다" 는 **사실만** 알리는 무바디 신호.
     *
     * 요청·응답 바디 없음(누구인지는 액세스 토큰으로 서버가 식별). 서버는 이를 받아 그 사용자의
     * "마지막 활동 시각" 을 갱신한다. 사후 전달의 INACTIVITY(장기 미사용 → 사망 추정 → 자동 전달)
     * 판정 기준이 이 시각이므로, 앱 실행/로그인 확정 시 1회 호출해 "아직 활동 중" 으로 미사용 타이머를
     * 리셋한다. 사용자가 앱을 오래 안 열면 ping 이 끊겨 시각이 굳고 → 미사용 기간이 쌓여 조건 충족 (이슈 #429).
     */
    @POST("users/me/activity")
    suspend fun logActivity(): BaseResponse<Unit>

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
}
