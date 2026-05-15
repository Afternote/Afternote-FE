package com.afternote.afternote_fe.screen.receiver.model

import androidx.compose.runtime.Immutable

/**
 * 수신자 홈 화면 UI 상태.
 *
 * 한 화면당 단일 UI State 객체로, 로드 상태 + 모든 기록 내려받기 다이얼로그 상태를 함께 보유한다.
 */
sealed interface ReceiverHomeUiState {
    data object Loading : ReceiverHomeUiState

    @Immutable
    data class Success(
        val senderName: String,
        val senderMessage: SenderMessage?,
        val mindRecord: MindRecordSummary,
        val timeLetterTotalCount: Int,
        val afternoteTotalCount: Int,
        val afternoteIcons: List<AfternoteSourceIcon>,
        val download: ReceiverDownloadState = ReceiverDownloadState.Idle,
    ) : ReceiverHomeUiState

    data class Error(
        val throwable: Throwable,
    ) : ReceiverHomeUiState
}

@Immutable
data class SenderMessage(
    val date: String,
    val body: String,
)

@Immutable
data class MindRecordSummary(
    val totalCount: Int,
    val dailyQuestionCount: Int,
    val diaryCount: Int,
    val deepThoughtCount: Int,
)

@Immutable
data class AfternoteSourceIcon(
    val key: String,
    val drawableResId: Int,
)

sealed interface ReceiverDownloadState {
    data object Idle : ReceiverDownloadState

    data object Confirming : ReceiverDownloadState

    data object InProgress : ReceiverDownloadState

    data object Done : ReceiverDownloadState

    data class Failed(
        val message: String,
    ) : ReceiverDownloadState
}
