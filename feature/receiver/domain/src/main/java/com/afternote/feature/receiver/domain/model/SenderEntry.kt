package com.afternote.feature.receiver.domain.model

/**
 * 받은 기록함에 사용자가 등록한 발신자 카드.
 *
 * [id]는 백엔드 발신자 ID가 아니라 이 로컬 카드의 안정적인 식별자다. 저장소가 이 값을
 * 프로세스 재시작 뒤에도 복원하므로, 발신자별 본인 확인 상태도 같은 ID로 다시 찾을 수 있다.
 *
 * [masterKey]와 신원 필드는 마스터 키 검증 성공 뒤 채워지고, [verificationStatus]는 가장 최근에
 * 조회한 열람 신청 상태를 보관한다. 카드 별칭인 [name]은 동명이인을 위해 중복을 허용한다.
 */
data class SenderEntry(
    val id: String,
    val name: String,
    val masterKey: String? = null,
    val realSenderName: String? = null,
    val relation: String? = null,
    val verificationStatus: DeliveryVerificationStatus? = null,
)
