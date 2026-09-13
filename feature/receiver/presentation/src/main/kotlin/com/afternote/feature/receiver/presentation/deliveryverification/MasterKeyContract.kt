package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface MasterKeyIntent : MviIntent {
    data class Submit(
        val senderId: String,
        val masterKey: String,
    ) : MasterKeyIntent

    data object ConsumeError : MasterKeyIntent

    data object ConsumeVerified : MasterKeyIntent
}

internal sealed interface MasterKeyReducerEvent : ReducerEvent {
    data object SubmissionStarted : MasterKeyReducerEvent

    data object Verified : MasterKeyReducerEvent

    data class ErrorRaised(
        val message: UiText,
        val finishesSubmission: Boolean = false,
    ) : MasterKeyReducerEvent

    data object ErrorConsumed : MasterKeyReducerEvent

    data object VerifiedConsumed : MasterKeyReducerEvent
}

internal fun reduceMasterKey(
    state: MasterKeyUiState,
    event: MasterKeyReducerEvent,
): MasterKeyUiState =
    when (event) {
        MasterKeyReducerEvent.SubmissionStarted -> {
            state.copy(isSubmitting = true, errorMessage = null)
        }

        MasterKeyReducerEvent.Verified -> {
            state.copy(isSubmitting = false, isVerified = true)
        }

        is MasterKeyReducerEvent.ErrorRaised -> {
            state.copy(
                errorMessage = event.message,
                isSubmitting =
                    state.isSubmitting && !event.finishesSubmission,
            )
        }

        MasterKeyReducerEvent.ErrorConsumed -> {
            state.copy(errorMessage = null)
        }

        MasterKeyReducerEvent.VerifiedConsumed -> {
            state.copy(isVerified = false)
        }
    }
