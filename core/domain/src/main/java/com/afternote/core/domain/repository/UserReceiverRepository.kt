package com.afternote.core.domain.repository

import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 계정에 등록된 수신자의 목록·CRUD·메시지·전달조건 계약 (#1282).
 *
 * `feature:receiver:domain` 의 `ReceiverRepository` 는 수신자 본인이 auth code 로 수신물을
 * 열람하는 흐름의 계약이라 대상이 다르다 — 여기는 로그인한 사용자(발신자)가 등록·관리하는 수신자다.
 */
interface UserReceiverRepository {
    /**
     * 수신인 목록. 로그인한 같은 세션의 일시적인 조회 실패는 이 구독자가 마지막으로 성공한 목록을 내고,
     * 로그아웃·새 세션·인증 실패에서는 빈 목록을 내 이전 계정의 수신인이 넘어가지 않게 한다.
     *
     * **빈 목록은 «수신인이 없음» 을 뜻하지 않는다 — 실패도 같은 모양으로 들어온다.** 화면이 이 값만 보고
     * «등록된 수신인이 없어요» 를 확정하면 오프라인에서 그 거짓을 사용자에게 보여 주게 된다. 실패와 없음을
     * 가르려면 [receiverListStateFlow] 를 쓴다.
     *
     * 로그인한 구독의 첫 방출은 조회 결과다. 조회 중에는 아무것도 내지 않으므로 `first()` 로 받는 호출처가
     * 요청 전에 끊기지 않는다.
     */
    val receiverListFlow: Flow<List<Receiver>>

    /**
     * [receiverListFlow] 와 같은 조회를 로딩·성공·실패로 갈라 낸다 (#2045). 두 Flow 는 한 원천에서 나오므로
     * 세션 격리, 늦은 결과 폐기, 갱신 revision 이 같다.
     *
     * 구독은 [ReceiverListState.Loading] 으로 시작하고, 같은 구독이 다시 조회할 때마다 Loading 을 먼저 낸 뒤
     * 결과를 낸다. 세션이 바뀌면 새 세션의 Loading 앞에 [ReceiverListState.SignedOut] 을 낸다.
     */
    val receiverListStateFlow: Flow<ReceiverListState>

    /**
     * 구독 중인 수신자 목록을 모두 다시 조회하게 한다 (#2045). 결과는 두 목록 Flow 로 온다. 새 구독은 이 호출
     * 없이도 조회한다.
     */
    fun refreshReceiverList()

    // 수신자 목록 조회
    suspend fun getReceivers(): List<Receiver>

    // 수신자 등록
    suspend fun createReceiver(
        name: String,
        relation: String,
        phone: String?,
        email: String,
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

    // 수신자별 전달조건 조회 (콘텐츠별)
    suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions

    // 수신자별 전달조건 설정/변경 (보낸 conditions 로 저장)
    suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        conditions: List<DeliveryConditionItem>,
    ): ReceiverDeliveryConditions
}
