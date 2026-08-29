package com.afternote.core.domain.repository

import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Passkey
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * 수신인 목록. 로그인한 같은 세션의 일시적인 조회 실패는 이 구독자가 마지막으로 성공한 목록을 내고,
     * 로그아웃·새 세션·인증 실패에서는 빈 목록을 내 이전 계정의 수신인이 넘어가지 않게 한다.
     *
     * **빈 목록은 «수신인이 없음» 을 뜻하지 않는다 — 실패도 같은 모양으로 들어온다.** 화면이 이 값만 보고
     * «등록된 수신인이 없어요» 를 확정하면 오프라인에서 그 거짓을 사용자에게 보여 주게 된다. 실패와 없음을
     * 가르려면 별도 신호가 필요하고, 그 표기는 #714 범위다.
     */
    val receiverListFlow: Flow<List<Receiver>>

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

    // 등록된 패스키 목록 조회
    suspend fun getPasskeys(): List<Passkey>

    /**
     * 패스키 등록용 challenge 옵션 조회 — 서버가 내려준 원본 JSON 문자열을 그대로 반환한다.
     * 호출부(Android Credential Manager)가 이 문자열을 그대로 `CreatePublicKeyCredentialRequest`에 전달한다.
     */
    suspend fun getPasskeyRegisterOptions(): String

    /**
     * Credential Manager가 반환한 등록 응답 JSON을 그대로 서버에 전달해 패스키 등록을 완료한다.
     */
    suspend fun registerPasskey(credentialJson: String): Passkey

    // 활동 기록(ping) — 앱 실행/로그인 확정 시 미사용(INACTIVITY) 전달조건 타이머를 리셋
    suspend fun logActivity()

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

    // 수신자별 전달조건 조회 (콘텐츠별)
    suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions

    // 수신자별 전달조건 설정/변경 (보낸 conditions 로 저장)
    suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        conditions: List<DeliveryConditionItem>,
    ): ReceiverDeliveryConditions
}
