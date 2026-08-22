package com.afternote.feature.afternote.presentation.receiver.senderdetail

import androidx.compose.runtime.Immutable

/**
 * 발신자 상세(designs 11·12) UI 상태.
 *
 * 서버-backed 카드는 `record-boxes` 응답의 발신자명·열람 상태·신청/승인 일시를 사용한다.
 * 마스터 키 입력 전 임시 카드는 SenderRegistry의 사용자 별칭을 사용한다.
 */
sealed interface SenderDetailUiState {
    data object Loading : SenderDetailUiState

    /**
     * 정상 로드 완료.
     *
     * @property displayName 서버 카드의 발신자명 또는 마스터 키 입력 전 로컬 카드의 사용자 별칭.
     * @property verification 열람 신청 상태 → 정보 박스 "상태" 행 + 하단 CTA 분기.
     * @property requestedAt 신청일(yyyy.MM.dd.) — 서버 기록함의 `requestedAt` 포맷팅 결과.
     *                            null이면 "신청 기록이 없습니다".
     * @property approvedAt 승인일(yyyy.MM.dd.) — 서버 기록함이 내려준 승인 시각. null 이면 "승인 기록이 없습니다".
     */
    @Immutable
    data class Success(
        val displayName: String,
        val verification: SenderVerificationState,
        val requestedAt: String?,
        val approvedAt: String?,
        /** "기록 열람하기" 성공 후 글로벌 헤더 컨텍스트 저장 완료 신호 — UI 가 수신자 홈으로 이동 후 소비. */
        val shouldOpenReceiverHome: Boolean = false,
    ) : SenderDetailUiState

    data object SenderNotFound : SenderDetailUiState
}

sealed interface SenderVerificationState {
    data object NotRequested : SenderVerificationState

    data object Pending : SenderVerificationState

    data object Approved : SenderVerificationState

    data object Rejected : SenderVerificationState
}
