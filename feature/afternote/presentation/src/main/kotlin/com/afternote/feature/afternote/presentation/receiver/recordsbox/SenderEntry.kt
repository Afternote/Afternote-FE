package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.compose.runtime.Immutable

/**
 * 받은 기록함 카드 한 줄의 표시 데이터.
 *
 * `id` 는 클라 로컬에서 발급하는 식별자(백엔드 발신자 ID 와 별개). 서버 매칭은 마스터 키 검증 시점에
 * `verify(authCode)` 응답으로 이뤄지므로, 카드 자체는 사용자가 부여한 *별칭* 만 보관한다.
 *
 * `lastConfirmedAt` 은 디자인 13 의 "마지막 확인: 2025.10.21." 텍스트에 표시되는 값. 백엔드 API
 * 미확정이라 현재(이슈 #215) stub registry 에서는 비워 둔다. 후속 단계에서 발신자 리스트 조회 API
 * 응답 필드로 채운다.
 */
@Immutable
data class SenderEntry(
    val id: String,
    val name: String,
    val lastConfirmedAt: String? = null,
)
