package com.afternote.feature.receiver.presentation.senderdetail

import androidx.compose.runtime.Immutable

/**
 * 발신자 상세(designs 11·12) UI 상태.
 *
 * 카드 별칭은 SenderRegistry, 신원(실명) 은 `verify(masterKey)` 응답, 신청·승인 일시는
 * `getDeliveryVerificationStatus()` 응답을 결합해 정보 박스 4 행 + CTA 분기 데이터를 만든다.
 */
sealed interface SenderDetailUiState {
    data object Loading : SenderDetailUiState

    /**
     * 정상 로드 완료.
     *
     * @property displayName 헤더에 노출할 발신자 표시명. `verify` 응답의 실명이 있으면 그것을, 없으면 사용자 별칭.
     * @property verification 열람 신청 상태 → 정보 박스 "상태" 행 + 하단 CTA 분기.
     * @property requestedAt 신청일(yyyy.MM.dd.) — `getDeliveryVerificationStatus().createdAt` 포맷팅 결과. null 이면 "신청 기록이 없습니다".
     * @property approvedAt 승인일(yyyy.MM.dd.) — APPROVED 상태일 때만 채워짐. null 이면 "승인 기록이 없습니다".
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

    /** 카드 별칭은 알지만 상태 조회 자체가 실패한 경우 — 정보 박스를 비워 둔 채 에러만 표시. */
    @Immutable
    data class StatusLoadFailed(
        val displayName: String,
    ) : SenderDetailUiState
}

sealed interface SenderVerificationState {
    data object NotRequested : SenderVerificationState

    data object Pending : SenderVerificationState

    data object Approved : SenderVerificationState

    data object Rejected : SenderVerificationState
}
