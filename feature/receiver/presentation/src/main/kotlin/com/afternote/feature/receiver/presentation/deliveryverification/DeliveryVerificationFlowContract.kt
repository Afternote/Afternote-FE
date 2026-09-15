package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal sealed interface DeliveryVerificationFlowIntent : MviIntent

internal data class DeliveryVerificationFlowUiState(
    val senderId: String,
    val isIdentityVerified: Boolean = false,
) : UiState

internal sealed interface DeliveryVerificationFlowReducerEvent : ReducerEvent {
    data class IdentityVerificationChanged(
        val isVerified: Boolean,
    ) : DeliveryVerificationFlowReducerEvent
}

internal fun reduceDeliveryVerificationFlow(
    state: DeliveryVerificationFlowUiState,
    event: DeliveryVerificationFlowReducerEvent,
): DeliveryVerificationFlowUiState =
    when (event) {
        is DeliveryVerificationFlowReducerEvent.IdentityVerificationChanged -> state.copy(isIdentityVerified = event.isVerified)
    }
