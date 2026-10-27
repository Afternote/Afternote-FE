package com.afternote.core.network.service

import com.afternote.core.network.dto.NotificationAnniversaryDto
import com.afternote.core.network.dto.NotificationAnniversaryRequestDto
import com.afternote.core.network.dto.NotificationDelegateDto
import com.afternote.core.network.dto.NotificationDelegateRequestDto
import com.afternote.core.network.dto.NotificationEntryDto
import com.afternote.core.network.dto.NotificationInboxDto
import com.afternote.core.network.dto.NotificationPreferencesDto
import com.afternote.core.network.dto.NotificationPreferencesRequestDto
import com.afternote.core.network.dto.NotificationTargetDto
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 서버 알림(BE#317 비결제 알림 A~E)의 사용자 쪽 API (#2321). 전부 인증이 필요하다.
 *
 * 기기 등록(`users/push-tokens`·`users/push-installations`)은 계정 API 와 수명이 같아
 * [UserApiService] 에 남긴다. 여기는 알림 그 자체 — 알림함·탭 목적지·앱 진입 문맥·알림 설정이다.
 */
interface NotificationApiService {
    /** 알림함. 0 부터, 한 페이지 50개, ID 내림차순. 취소된 알림은 빠진다. */
    @GET("users/notifications")
    suspend fun getInbox(
        @Query("page") page: Int,
    ): BaseResponse<NotificationInboxDto>

    /** 알림을 탭한 시점의 현재 목적지. 지워진 대상이면 목록과 안내 문구를 준다. */
    @GET("users/notifications/{id}/target")
    suspend fun getTarget(
        @Path("id") notificationId: Long,
    ): BaseResponse<NotificationTargetDto>

    /** 본인 알림 읽음 처리. */
    @PATCH("users/notifications/{id}/read")
    suspend fun markRead(
        @Path("id") notificationId: Long,
    ): BaseResponse<Unit>

    /**
     * 앱 실행·복귀 문맥. 이전 방문 기준 30일 복귀 여부와 쉬운 질문, 대리인 절차 취소 결과를 주고
     * 서버의 활동 시각을 갱신한다. 종료 상태에서의 실행·로그인 직후에 부른다.
     */
    @POST("users/me/notification-preferences/entry")
    suspend fun reportEntry(): BaseResponse<NotificationEntryDto>

    @GET("users/me/notification-preferences")
    suspend fun getPreferences(): BaseResponse<NotificationPreferencesDto>

    /** 바꾸면 서버가 이전 대기 중인 기록 알림(B)을 취소한다. */
    @PUT("users/me/notification-preferences")
    suspend fun updatePreferences(
        @Body request: NotificationPreferencesRequestDto,
    ): BaseResponse<NotificationPreferencesDto>

    @GET("users/me/notification-preferences/delegate")
    suspend fun getDelegate(): BaseResponse<NotificationDelegateDto>

    /** 바꾸면 이전 대리인에게 간 확인 링크가 무효가 된다. */
    @PUT("users/me/notification-preferences/delegate")
    suspend fun updateDelegate(
        @Body request: NotificationDelegateRequestDto,
    ): BaseResponse<Unit>

    @DELETE("users/me/notification-preferences/delegate")
    suspend fun deleteDelegate(): BaseResponse<Unit>

    @GET("users/me/notification-preferences/anniversaries")
    suspend fun getAnniversaries(): BaseResponse<List<NotificationAnniversaryDto>>

    @POST("users/me/notification-preferences/anniversaries")
    suspend fun createAnniversary(
        @Body request: NotificationAnniversaryRequestDto,
    ): BaseResponse<NotificationAnniversaryDto>

    @DELETE("users/me/notification-preferences/anniversaries/{id}")
    suspend fun deleteAnniversary(
        @Path("id") anniversaryId: Long,
    ): BaseResponse<Unit>
}
