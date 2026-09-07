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

    /**
     * 재시도로 풀릴 수 있는 실패 — 공통 오류 팝업으로 안내한다 (#446 · #1737).
     *
     * 「다시 시도하기」 는 처음부터가 아니라 **실패한 그 단계**를 다시 건다. 그 단계를
     * 상태에 담지 않는 이유는 재시도가 화면의 결정이 아니라서다 — 무엇을 다시 걸지는
     * ViewModel 이 마지막 시도로 들고 있고, 화면은 갈래만 보고 팝업을 고른다.
     */
    data class FailedWithRetry(
        val popup: ReceiverDownloadErrorPopup,
    ) : ReceiverDownloadState

    /**
     * 재시도해도 같은 결과인 실패 — 팝업 대신 기존 안내를 유지한다.
     *
     * 미구현 내보내기([com.afternote.feature.receiver.domain.error.ReceiverFailure.ExportNotSupported],
     * #1726)가 여기 온다. 「다시 시도하기」 를 주면 같은 실패를 반복하게 만든다.
     */
    data class Failed(
        @param:StringRes val messageRes: Int,
    ) : ReceiverDownloadState
}

/**
 * 내려받기 실패를 그릴 공통 오류 팝업의 갈래 (#446).
 *
 * 수신자 화면의 `ReceiverErrorPopup` 과 같은 모델이지만 그쪽은 receiver 모듈 안에 갇혀 있고
 * (`internal`), 갈래도 다르다 — 이쪽은 업로드가 아니라 **내려받은 파일을 로컬에 저장**하는
 * 단계가 따로 있다.
 */
enum class ReceiverDownloadErrorPopup {
    /** 서버에 닿지도 못한 실패 — 안내는 "연결을 확인하라". */
    NETWORK,

    /** 서버가 응답했지만 내려주지 못한 실패 — 안내는 "잠시 후 다시". */
    SERVER,

    /** 내려받은 뒤 파일로 저장하지 못한 실패 — 서버 문제가 아니므로 갈라 둔다. */
    SAVE,
}
