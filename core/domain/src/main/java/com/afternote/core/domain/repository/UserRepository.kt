package com.afternote.core.domain.repository

import com.afternote.core.model.user.DeliveryCondition
import com.afternote.core.model.user.DeliveryConditionType
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * 로컬 캐시된 *passkey 등록 여부* Flow. UI 가 진입 즉시 placeholder 로 표시하기 위함.
     * DataStore 같은 인프라 디테일은 본 Repository 가 흡수 — UI/도메인은 Flow 만 소비.
     */
    fun isPasskeyRegisteredFlow(): Flow<Boolean>

    // 수신자 목록 조회
    suspend fun getReceivers(): List<Receiver>

    // 수신자 등록
    suspend fun createReceiver(
        name: String,
        relation: String,
        phone: String?,
        email: String?,
        message: String?,
    ): ReceiverCreated

    // 수신자 상세 조회
    suspend fun getReceiverDetail(receiverId: Long): ReceiverDetail

    // 수신자 정보 수정
    suspend fun updateReceiver(
        receiverId: Long,
        name: String,
        phone: String,
        relation: String,
        email: String,
    ): Receiver

    // 수신자 메시지 수정
    suspend fun updateReceiverMessage(
        receiverId: Long,
        message: String,
    )

    // 내 프로필 조회
    suspend fun getMyProfile(): User

    // 프로필 수정
    suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User

    // 회원 탈퇴
    suspend fun deleteAccount()

    // 푸시 알림 설정 조회
    suspend fun getMyPushSettings(): UserPushSetting

    // 푸시 알림 설정 수정
    suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting

    // 연결된 계정 조회
    suspend fun getConnectedAccounts(): UserConnectedAccount

    // 소셜 계정 연결
    suspend fun linkConnectedAccount(
        provider: String,
        accessToken: String,
    ): UserConnectedAccount

    // 소셜 계정 연결 해제
    suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount

    // 전달 조건 조회
    suspend fun getDeliveryCondition(): DeliveryCondition

    // 전달 조건 수정
    suspend fun updateDeliveryCondition(
        conditionType: DeliveryConditionType,
        inactivityPeriodDays: Int?,
        specificDate: String?,
    ): DeliveryCondition
}
