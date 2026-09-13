package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal data class PassKeyUiState(
    val isRegistering: Boolean = false,
    val registrationId: Long = 0,
    val result: PasskeyRegistrationResult? = null,
) : UiState

internal sealed interface PassKeyIntent : MviIntent {
    // Activity 소유 플랫폼 요청은 화면이 제공하고 등록 중에만 보유한다. 화면 폐기 시 CancelRegistration으로 해제한다.
    data class Register(
        val createCredential: suspend (optionsJson: String) -> String,
    ) : PassKeyIntent

    data object CancelRegistration : PassKeyIntent

    data class ConsumeResult(
        val result: PasskeyRegistrationResult,
    ) : PassKeyIntent
}

internal sealed interface PassKeyReducerEvent : ReducerEvent {
    data object Started : PassKeyReducerEvent

    data class Finished(
        val registrationId: Long,
        val result: PasskeyRegistrationResult,
    ) : PassKeyReducerEvent

    data class Stopped(
        val registrationId: Long,
    ) : PassKeyReducerEvent

    data object Canceled : PassKeyReducerEvent

    data class Consumed(
        val result: PasskeyRegistrationResult,
    ) : PassKeyReducerEvent
}
