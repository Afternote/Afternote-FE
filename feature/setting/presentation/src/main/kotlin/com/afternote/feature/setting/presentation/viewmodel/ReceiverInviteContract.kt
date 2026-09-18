package com.afternote.feature.setting.presentation.viewmodel

import androidx.compose.runtime.Immutable
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

/** 수신자 등록 화면의 카카오톡 초대 시트(4996:39786) 의도 (#944). */
sealed interface ReceiverInviteIntent : MviIntent {
    /** «카카오톡으로 초대 보내기» CTA — 폼의 이름을 들고 시트를 연다. */
    data class OpenSheet(
        val receiverName: String,
    ) : ReceiverInviteIntent

    /** «나중에 보내기»·시트 밖 탭. */
    data object CloseSheet : ReceiverInviteIntent

    /** 시트의 «카카오톡으로 초대 보내기» — 초대를 만들고 공유 요청을 낸다. */
    data object SendInvite : ReceiverInviteIntent

    /** 화면이 카카오 공유를 띄웠다 — 시트를 닫고 «초대를 보냈어요» 로 넘어간다. */
    data object ShareLaunched : ReceiverInviteIntent

    /** 화면이 카카오 공유를 띄우지 못했다 — 시트를 유지한 채 안내를 얹고, 원인은 진단에 남긴다. */
    data class ShareFailed(
        val message: UiText,
        val cause: Throwable,
    ) : ReceiverInviteIntent

    /** «초대 링크 다시 보내기» — 새 초대를 만들지 않고 같은 토큰으로 공유 요청을 다시 낸다. */
    data object Resend : ReceiverInviteIntent

    data object ConsumeShareRequest : ReceiverInviteIntent

    data object ConsumeError : ReceiverInviteIntent
}

/**
 * 화면이 카카오 SDK 로 공유를 띄우는 데 필요한 값. SDK 가 Activity 컨텍스트를 요구해 ViewModel 이
 * 직접 띄우지 않고 신호로 넘긴다 — 소셜 로그인의 토큰 획득이 UI 계층에 있는 것과 같은 이유다.
 *
 * @property token 초대 토큰(비밀값). 로그·리포팅에 남기지 않는다.
 * @property senderName 템플릿 제목 «{senderName}님이 …» 에 실리는 내 표시 이름.
 */
@Immutable
data class ReceiverInviteShareRequest(
    val token: String,
    val senderName: String,
    val receiverName: String,
)

/**
 * @property sheetReceiverName 시트가 열려 있으면 시트 본문에 쓸 수신자 이름, 닫혀 있으면 null.
 * @property shareRequest 초대가 만들어졌다 — 화면이 공유를 띄우고 소비한다.
 * @property sentInvitation 공유를 띄운 초대. 있으면 등록 화면이 폼 대신 «초대를 보냈어요»(4996:39909) 를
 *   그린다 — 별도 destination 이 아니라 같은 화면의 phase 다(설정 네비게이션 축은 #1695 소유).
 */
@Immutable
data class ReceiverInviteUiState(
    val sheetReceiverName: String? = null,
    val isCreating: Boolean = false,
    val shareRequest: ReceiverInviteShareRequest? = null,
    val sentInvitation: ReceiverInviteShareRequest? = null,
    val errorMessage: UiText? = null,
) : UiState

sealed interface ReceiverInviteReducerEvent : ReducerEvent {
    data class SheetOpened(
        val receiverName: String,
    ) : ReceiverInviteReducerEvent

    data object SheetClosed : ReceiverInviteReducerEvent

    data object Creating : ReceiverInviteReducerEvent

    data class Created(
        val request: ReceiverInviteShareRequest,
    ) : ReceiverInviteReducerEvent

    data class CreateFailed(
        val message: UiText,
    ) : ReceiverInviteReducerEvent

    data class ShareFailed(
        val message: UiText,
    ) : ReceiverInviteReducerEvent

    /** 공유가 떴다 — 시트를 닫고 보냈어요 phase 로. */
    data class Sent(
        val invitation: ReceiverInviteShareRequest,
    ) : ReceiverInviteReducerEvent

    /** «다시 보내기» — 같은 값으로 공유 신호만 다시 올린다. */
    data class ResendRequested(
        val request: ReceiverInviteShareRequest,
    ) : ReceiverInviteReducerEvent

    data object ShareRequestConsumed : ReceiverInviteReducerEvent

    data object ErrorConsumed : ReceiverInviteReducerEvent
}
