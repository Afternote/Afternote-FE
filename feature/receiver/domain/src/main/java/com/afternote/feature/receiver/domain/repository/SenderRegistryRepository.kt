package com.afternote.feature.receiver.domain.repository

import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderEntry
import kotlinx.coroutines.flow.Flow

/**
 * 받은 기록함 발신자 카드를 저장하고 관찰하는 계약.
 *
 * 이 계약은 로컬 카드 자체의 수명만 다룬다. 수신자별 저장 격리(#598)는 별도 계약이며,
 * 여기에는 수신자 식별자를 추가하지 않는다.
 */
interface SenderRegistryRepository {
    /** 등록 순서를 유지한 발신자 카드 목록. 읽기 실패 시 구현은 안전한 빈 목록을 방출한다. */
    val senders: Flow<List<SenderEntry>>

    /** 사용자가 지정한 카드 별칭 [name]으로 새 로컬 카드를 등록한다. */
    suspend fun register(name: String): Result<SenderEntry>

    /** 로컬 카드 ID로 조회한다. 일치하는 카드가 없으면 성공한 `null`을 반환한다. */
    suspend fun findById(id: String): Result<SenderEntry?>

    /** 마스터 키 검증 결과를 카드에 결합한다. 카드가 없으면 성공한 `null`을 반환한다. */
    suspend fun attachIdentity(
        id: String,
        masterKey: String,
        identity: ReceiverIdentity,
    ): Result<SenderEntry?>

    /** 최근 열람 신청 [status]를 카드에 기록한다. 카드가 없으면 성공한 `null`을 반환한다. */
    suspend fun updateVerificationStatus(
        id: String,
        status: DeliveryVerificationStatus,
    ): Result<SenderEntry?>
}
