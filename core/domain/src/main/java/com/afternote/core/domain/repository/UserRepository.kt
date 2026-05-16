package com.afternote.core.domain.repository

import com.afternote.core.model.ReceiverDailyQuestionsResult
import com.afternote.core.model.ReceiverMindRecordsResult
import com.afternote.core.model.setting.ConnectedAccounts
import com.afternote.core.model.setting.DeliveryCondition
import com.afternote.core.model.setting.DeliveryConditionType
import com.afternote.core.model.setting.PushSettings
import com.afternote.core.model.setting.ReceiverDetail
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.UserProfileModel

// TODO:검토
interface UserRepository {
    suspend fun getMyProfile(): Result<UserProfileModel>

    /**
     * 마지막으로 성공한 [getMyProfile]/[updateMyProfile]에서 캐싱한 사용자 이름을 즉시 반환한다.
     * 홈 콜드스타트 placeholder 용도이며, 캐시 미존재 또는 읽기 실패 시 null.
     */
    suspend fun getCachedUserName(): String?

    /** 로그아웃·회원 탈퇴 시 호출해 사용자 프로필 캐시를 비운다. */
    suspend fun clearCachedProfile()

    suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): Result<UserProfileModel>

    /**
     * DELETE /users/me — 회원 탈퇴. 로그인한 사용자의 계정을 삭제합니다. 모든 데이터가 영구 삭제되며 복구할 수 없습니다.
     */
    suspend fun withdrawAccount(): Result<Unit>

    /** GET /users/push-settings — 푸시 알림 설정 조회. */
    suspend fun getMyPushSettings(): Result<PushSettings>

    suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): Result<PushSettings>

    /** GET /users/receivers — 수신인 목록 조회. 로그인한 사용자가 등록한 수신인 목록을 조회합니다. */
    suspend fun getReceivers(): Result<List<ReceiverListItem>>

    suspend fun registerReceiver(
        name: String,
        relation: String,
        phone: String?,
        email: String?,
    ): Result<Long>

    suspend fun getReceiverDetail(receiverId: Long): Result<ReceiverDetail>

    suspend fun updateReceiver(
        receiverId: Long,
        name: String,
        relation: String,
        phone: String?,
        email: String?,
    ): Result<Unit>

    suspend fun getReceiverDailyQuestions(
        receiverId: Long,
        page: Int,
        size: Int,
    ): Result<ReceiverDailyQuestionsResult>

    /**
     * 수신인별 마음의 기록 전체 조회 (일기, 깊은 생각, 데일리 질문 답변).
     * GET /users/receivers/{receiverId}/mind-records
     */
    suspend fun getReceiverMindRecords(
        receiverId: Long,
        page: Int,
        size: Int,
    ): Result<ReceiverMindRecordsResult>

    /**
     * GET /users/delivery-condition — 로그인한 사용자의 전달 조건 설정 조회.
     */
    suspend fun getDeliveryCondition(): Result<DeliveryCondition>

    /**
     * PATCH /users/delivery-condition — 로그인한 사용자의 전달 조건 설정/변경.
     *
     * @param conditionType 전달 조건 타입
     * @param inactivityPeriodDays 비활동 기간(일), INACTIVITY일 때 사용
     * @param specificDate 특정 날짜(yyyy-MM-dd), SPECIFIC_DATE일 때 사용
     * @param leaveMessage 마지막 인사말 (수신자에게 전달되는 메시지)
     */
    suspend fun updateDeliveryCondition(
        conditionType: DeliveryConditionType,
        inactivityPeriodDays: Int?,
        specificDate: String?,
        leaveMessage: String? = null,
    ): Result<DeliveryCondition>

    /** GET /users/connected-accounts — 연동된 계정 (로컬·소셜) 조회. */
    suspend fun getConnectedAccounts(): Result<ConnectedAccounts>

    /**
     * POST /users/connected-accounts/{provider} — 신규 소셜 계정 연동.
     *
     * @param provider google/naver/kakao/apple
     * @param accessToken 소셜 SDK 로 받은 access token
     */
    suspend fun connectAccount(
        provider: String,
        accessToken: String,
    ): Result<ConnectedAccounts>

    /** DELETE /users/connected-accounts/{provider} — 소셜 계정 연동 해제. */
    suspend fun disconnectAccount(provider: String): Result<ConnectedAccounts>
}
