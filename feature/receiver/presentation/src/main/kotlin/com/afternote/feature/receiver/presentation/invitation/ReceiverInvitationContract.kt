package com.afternote.feature.receiver.presentation.invitation

import androidx.compose.runtime.Immutable
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopup

/** 초대 랜딩(4996:40023)의 사용자 의도 (#944). */
sealed interface ReceiverInvitationIntent : MviIntent {
    /** «카카오로 시작하고 수락하기» — 로그인 여부 판정은 화면 밖(앱 셸)이 하고, 로그인 상태에서만 온다. */
    data object Accept : ReceiverInvitationIntent

    /** «나중에 결정하기» — 토큰은 남기고 화면만 닫는다. */
    data object Defer : ReceiverInvitationIntent

    /** 안내로 끝난 초대의 «확인» — 토큰은 이미 지워져 있고 화면만 닫는다. */
    data object AcknowledgeNotice : ReceiverInvitationIntent

    /** 오류 팝업의 «다시 시도» — 조회 실패면 조회를, 수락 실패면 수락을 다시 한다. */
    data object Retry : ReceiverInvitationIntent

    data object DismissErrorPopup : ReceiverInvitationIntent

    data object ConsumeAccepted : ReceiverInvitationIntent

    data object ConsumeOpenReceivedRecords : ReceiverInvitationIntent

    data object ConsumeLoginRequired : ReceiverInvitationIntent

    data object ConsumeClose : ReceiverInvitationIntent
}

/** 랜딩이 그리는 본문의 갈래. */
sealed interface ReceiverInvitationPhase {
    /** 조회 중 — 초대자 이름을 아직 모른다. */
    data object Loading : ReceiverInvitationPhase

    /** 초대자를 확인했다 — 수락 버튼을 보인다. */
    data class Ready(
        val inviterName: String,
    ) : ReceiverInvitationPhase

    /**
     * 더 진행할 수 없는 초대(없음·만료·다른 사용자가 수락·본인 초대) — 안내 한 줄과 «확인» 만 보인다.
     * 이 갈래에 들어올 때 토큰은 이미 지워졌다.
     */
    data class Notice(
        val message: UiText,
    ) : ReceiverInvitationPhase
}

/**
 * 초대 랜딩 상태 (#944). 일회성 신호는 nullable 필드로 담고 `Consume*` Intent 로 비운다(#228).
 *
 * @property acceptedInviterName 수락 성공 — 완료 화면(4996:39921)으로 이 이름을 들고 간다.
 * @property openReceivedRecords 이미 등록된 수신자였다 — 안내 없이 받은 기록함으로 보낸다.
 * @property loginRequired 서버가 인증을 거절했다 — 토큰은 남기고 로그인으로 보낸다.
 * @property close «나중에 결정하기»·안내 확인 — 화면을 닫는다.
 */
@Immutable
data class ReceiverInvitationUiState(
    val phase: ReceiverInvitationPhase = ReceiverInvitationPhase.Loading,
    val isAccepting: Boolean = false,
    val errorPopup: ReceiverErrorPopup? = null,
    val acceptedInviterName: String? = null,
    val openReceivedRecords: Boolean = false,
    val loginRequired: Boolean = false,
    val close: Boolean = false,
) : UiState

sealed interface ReceiverInvitationReducerEvent : ReducerEvent {
    data object LookupStarted : ReceiverInvitationReducerEvent

    data class LookupSucceeded(
        val inviterName: String,
    ) : ReceiverInvitationReducerEvent

    data class NoticeShown(
        val message: UiText,
    ) : ReceiverInvitationReducerEvent

    data class ErrorPopupShown(
        val popup: ReceiverErrorPopup,
    ) : ReceiverInvitationReducerEvent

    data object ErrorPopupDismissed : ReceiverInvitationReducerEvent

    data object AcceptStarted : ReceiverInvitationReducerEvent

    data class AcceptSucceeded(
        val inviterName: String,
    ) : ReceiverInvitationReducerEvent

    data object AcceptFinished : ReceiverInvitationReducerEvent

    data object AlreadyRegistered : ReceiverInvitationReducerEvent

    data object LoginRequired : ReceiverInvitationReducerEvent

    data object CloseRequested : ReceiverInvitationReducerEvent

    data object AcceptedConsumed : ReceiverInvitationReducerEvent

    data object OpenReceivedRecordsConsumed : ReceiverInvitationReducerEvent

    data object LoginRequiredConsumed : ReceiverInvitationReducerEvent

    data object CloseConsumed : ReceiverInvitationReducerEvent
}
