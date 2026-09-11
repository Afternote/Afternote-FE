package com.afternote.feature.home.presentation.receiver.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.afternote.core.ui.icon.AfternoteSourceIcon

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
        /** null은 조회 실패, 0은 정상 조회된 빈 목록이다. */
        val mindRecord: MindRecordSummary?,
        /** null은 조회 실패, 0은 정상 조회된 빈 목록이다. */
        val timeLetterTotalCount: Int?,
        /** null은 조회 실패, 0은 정상 조회된 빈 목록이다. */
        val afternoteTotalCount: Int?,
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
    val dailyQuestionCount: Int,
    val diaryCount: Int,
) {
    val totalCount: Int
        get() = dailyQuestionCount + diaryCount
}

@Immutable
sealed interface ReceiverDownloadState {
    data object Idle : ReceiverDownloadState

    data object Confirming : ReceiverDownloadState

    data object InProgress : ReceiverDownloadState

    data object Done : ReceiverDownloadState

    data class Failed(
        @param:StringRes val messageRes: Int,
    ) : ReceiverDownloadState
}
