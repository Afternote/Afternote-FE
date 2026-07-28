package com.afternote.core.network.service

import com.afternote.core.network.dto.DeliveryConditionDto
import com.afternote.core.network.dto.DeliveryConditionRequestDto
import com.afternote.core.network.dto.ReceiverDetailDto
import com.afternote.core.network.dto.ReceiverListDto
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.UserConnectedAccountDto
import com.afternote.core.network.dto.UserCreateReceiverDto
import com.afternote.core.network.dto.UserCreateReceiverRequestDto
import com.afternote.core.network.dto.UserDto
import com.afternote.core.network.dto.UserPatchReceiverDto
import com.afternote.core.network.dto.UserPatchReceiverRequestDto
import com.afternote.core.network.dto.UserPushSettingDto
import com.afternote.core.network.dto.UserUpdateProfileRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
import com.afternote.core.network.dto.UserUpdateReceiverMessageRequestDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionUpdateRequestDto
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
    suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>>

    // 수신자 등록
    @POST("users/receivers")
    suspend fun createReceiver(
        @Body request: UserCreateReceiverRequestDto,
    ): BaseResponse<UserCreateReceiverDto>

    // 수신자 상세 조회
    @GET("users/receivers/{receiverId}")
    suspend fun getReceiverDetail(
        @Path("receiverId") receiverId: Long,
    ): BaseResponse<ReceiverDetailDto>

    // 수신자 정보 수정
    @PATCH("users/receivers/{receiverId}")
    suspend fun updateReceiver(
        @Path("receiverId") receiverId: Long,
        @Body request: UserPatchReceiverRequestDto,
    ): BaseResponse<UserPatchReceiverDto>

    // 수신자 메시지 수정
    @PATCH("users/receivers/{receiverId}/message")
    suspend fun updateReceiverMessage(
        @Path("receiverId") receiverId: Long,
        @Body request: UserUpdateReceiverMessageRequestDto,
    ): BaseResponse<Unit>

    // 내 프로필 조회
    @GET("users/me")
    suspend fun getMyProfile(): BaseResponse<UserDto>

    // 프로필 수정
    @PATCH("users/me")
    suspend fun updateMyProfile(
        @Body request: UserUpdateProfileRequestDto,
    ): BaseResponse<UserDto>

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
    suspend fun getMyPushSettings(): BaseResponse<UserPushSettingDto>

    // 푸시 알림 설정 수정
    @PATCH("users/push-settings")
    suspend fun updateMyPushSettings(
        @Body request: UserUpdatePushSettingRequestDto,
    ): BaseResponse<UserPushSettingDto>

    // 연결된 계정 조회
    @GET("users/connected-accounts")
    suspend fun getConnectedAccounts(): BaseResponse<UserConnectedAccountDto>

    // 소셜 계정 연결
    @POST("users/connected-accounts/{provider}")
    suspend fun linkConnectedAccount(
        @Path("provider") provider: String,
        @Body request: SocialAccountLinkRequestDto,
    ): BaseResponse<UserConnectedAccountDto>

    // 소셜 계정 연결 해제
    @DELETE("users/connected-accounts/{provider}")
    suspend fun unlinkConnectedAccount(
        @Path("provider") provider: String,
    ): BaseResponse<UserConnectedAccountDto>

    // 전달 조건 조회
    @GET("users/delivery-condition")
    suspend fun getDeliveryCondition(): BaseResponse<DeliveryConditionDto>

    // 전달 조건 수정
    @PATCH("users/delivery-condition")
    suspend fun updateDeliveryCondition(
        @Body request: DeliveryConditionRequestDto,
    ): BaseResponse<DeliveryConditionDto>

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
    ): BaseResponse<ReceiverDeliveryConditionDto>

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
        @Body request: ReceiverDeliveryConditionUpdateRequestDto,
    ): BaseResponse<ReceiverDeliveryConditionDto>
}
