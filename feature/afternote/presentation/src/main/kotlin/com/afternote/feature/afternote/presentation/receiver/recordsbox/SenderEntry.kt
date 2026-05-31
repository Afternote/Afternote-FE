package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.compose.runtime.Immutable
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus

/**
 * 받은 기록함 카드 한 줄의 표시 데이터.
 *
 * `id` 는 클라 로컬에서 발급하는 식별자(백엔드 발신자 ID 와 별개). 서버 매칭은 마스터 키 검증 시점에
 * `verify(authCode)` 응답으로 이뤄지므로, 카드 자체는 사용자가 부여한 *별칭* 만 보관한다.
 *
 * `authCode` 는 마스터 키 검증 성공 후 채워지며, "기록 열람하기" 진입 시 [com.afternote.feature.afternote
 * .domain.repository.receiver.ReceiverRepository.saveAuthCode] 로 글로벌 헤더 컨텍스트에 복원한다.
 * `realSenderName`·`relation` 은 [com.afternote.feature.receiver.domain.model.ReceiverIdentity]
 * 응답에서 수신한 값으로 발신자 상세(11·12)의 인사말 및 관계 표기에 사용한다.
 *
 * `verificationStatus` 는 가장 최근에 조회한 열람 신청 상태 캐시. 발신자 상세 진입 시 서버 호출 결과로
 * 갱신되며, 카드 리스트 자체에서는 표시하지 않는다.
 *
 * `lastConfirmedAt` 은 디자인 13 의 "마지막 확인: 2025.10.21." 텍스트에 표시되는 값. 백엔드 API
 * 미확정이라 현재(이슈 #215) stub registry 에서는 비워 둔다. 후속 단계에서 발신자 리스트 조회 API
 * 응답 필드로 채운다.
 */
@Immutable
data class SenderEntry(
    val id: String,
    val name: String,
    val authCode: String? = null,
    val realSenderName: String? = null,
    val relation: String? = null,
    val verificationStatus: DeliveryVerificationStatus? = null,
    val lastConfirmedAt: String? = null,
)
