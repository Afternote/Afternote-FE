package com.afternote.feature.afternote.presentation.receiver.summary

/**
 * 수신자 홈 화면에 표시할 요약 정보.
 *
 * Route에서 ViewModel 데이터를 수집한 뒤 Stateless Screen에 전달합니다.
 */
data class ReceiverSummaryUiState(
    val authCode: String = "",
    val senderName: String = "",
    val leaveMessage: String? = null,
    val mindRecordTotalCount: Int = 0,
    val timeLetterTotalCount: Int = 0,
    val afternoteTotalCount: Int = 0,
)
