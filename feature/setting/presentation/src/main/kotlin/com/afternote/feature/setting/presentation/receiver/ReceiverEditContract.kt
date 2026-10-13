package com.afternote.feature.setting.presentation.receiver

import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ReceiverEditIntent : MviIntent {
    data class Update(
        val name: String,
        val relation: String,
        val phone: String,
        val email: String,
        val message: String,
    ) : ReceiverEditIntent

    data object ConsumeSuccess : ReceiverEditIntent
}

internal sealed interface ReceiverEditReducerEvent : ReducerEvent {
    data class Loaded(
        val receiver: ReceiverDetail,
    ) : ReceiverEditReducerEvent

    data class LoadFailed(
        val message: UiText,
    ) : ReceiverEditReducerEvent

    /** 요청을 보내기 전의 입력 형식 검증 실패. 서버 저장 실패([SaveFailed])와 구분한다. */
    data class ValidationFailed(
        val message: UiText,
    ) : ReceiverEditReducerEvent

    data object Saving : ReceiverEditReducerEvent

    data object Saved : ReceiverEditReducerEvent

    data class SaveFailed(
        val message: UiText,
    ) : ReceiverEditReducerEvent

    data object SuccessConsumed : ReceiverEditReducerEvent
}
