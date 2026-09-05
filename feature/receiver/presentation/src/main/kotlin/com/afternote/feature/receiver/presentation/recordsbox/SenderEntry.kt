package com.afternote.feature.receiver.presentation.recordsbox

import androidx.compose.runtime.Immutable
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus

/**
 * 받은 기록함 카드 한 줄의 표시 데이터.
 *
 * `id` 는 클라 로컬에서 발급하는 식별자(백엔드 발신자 ID 와 별개). 서버 매칭은 마스터 키 검증 시점에
 * `verify(masterKey)` 응답으로 이뤄지므로, 카드 자체는 사용자가 부여한 *별칭* 만 보관한다.
 *
 * `masterKey` 는 마스터 키 검증 성공 후 채워지며, "기록 열람하기" 진입 시 [com.afternote.feature.afternote
 * .domain.repository.receiver.ReceiverRepository.saveMasterKey] 로 글로벌 헤더 컨텍스트에 복원한다.
 * `realSenderName`·`relation` 은 [com.afternote.feature.receiver.domain.model.ReceiverIdentity]
 * 응답에서 수신한 값으로 발신자 상세(11·12)의 인사말 및 관계 표기에 사용한다.
 *
 * `verificationStatus` 는 가장 최근에 조회한 열람 신청 상태 캐시. 발신자 상세 진입 시 서버 호출 결과로
 * 갱신되며, 카드 리스트 자체에서는 표시하지 않는다.
 */
@Immutable
data class SenderEntry(
    val id: String,
    val name: String,
    val masterKey: String? = null,
    val realSenderName: String? = null,
    val relation: String? = null,
    val verificationStatus: DeliveryVerificationStatus? = null,
)
