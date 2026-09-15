package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopup

internal sealed interface IdentityVerificationIntent : MviIntent {
    data class UpdateEmail(
        val value: String,
    ) : IdentityVerificationIntent

    data class UpdateCode(
        val value: String,
    ) : IdentityVerificationIntent

    data object RequestCode : IdentityVerificationIntent

    data class Verify(
        val senderId: String,
    ) : IdentityVerificationIntent

    data object RetryFailedRequest : IdentityVerificationIntent

    data object DismissErrorPopup : IdentityVerificationIntent

    data object ConsumeError : IdentityVerificationIntent

    data object ConsumeVerified : IdentityVerificationIntent
}

internal sealed interface IdentityVerificationReducerEvent : ReducerEvent {
    data class EmailChanged(
        val value: String,
    ) : IdentityVerificationReducerEvent

    data class CodeChanged(
        val value: String,
    ) : IdentityVerificationReducerEvent

    data object CodeSending : IdentityVerificationReducerEvent

    data object CodeSent : IdentityVerificationReducerEvent

    data object CodeSendFinished : IdentityVerificationReducerEvent

    data object VerificationStarted : IdentityVerificationReducerEvent

    data object Verified : IdentityVerificationReducerEvent

    data object VerificationFinished : IdentityVerificationReducerEvent

    data class ErrorRaised(
        val message: UiText,
    ) : IdentityVerificationReducerEvent

    data class PopupRaised(
        val popup: ReceiverErrorPopup,
    ) : IdentityVerificationReducerEvent

    data object PopupDismissed : IdentityVerificationReducerEvent

    data object ErrorConsumed : IdentityVerificationReducerEvent

    data object VerifiedConsumed : IdentityVerificationReducerEvent
}

internal fun reduceIdentityVerification(
    state: IdentityVerificationUiState,
    event: IdentityVerificationReducerEvent,
): IdentityVerificationUiState =
    when (event) {
        is IdentityVerificationReducerEvent.EmailChanged -> {
            state.copy(
                email = event.value,
                isEmailFormatValid = EMAIL_REGEX.matches(event.value.trim()),
                errorMessage = null,
            )
        }

        is IdentityVerificationReducerEvent.CodeChanged -> {
            state.copy(code = event.value, errorMessage = null)
        }

        IdentityVerificationReducerEvent.CodeSending -> {
            state.copy(isSendingCode = true, errorMessage = null)
        }

        IdentityVerificationReducerEvent.CodeSent -> {
            state.copy(isSendingCode = false, isVerificationSent = true)
        }

        IdentityVerificationReducerEvent.CodeSendFinished -> {
            state.copy(isSendingCode = false)
        }

        IdentityVerificationReducerEvent.VerificationStarted -> {
            state.copy(isVerifying = true, errorMessage = null)
        }

        IdentityVerificationReducerEvent.Verified -> {
            state.copy(isVerifying = false, isVerified = true)
        }

        IdentityVerificationReducerEvent.VerificationFinished -> {
            state.copy(isVerifying = false)
        }

        is IdentityVerificationReducerEvent.ErrorRaised -> {
            state.copy(errorMessage = event.message)
        }

        is IdentityVerificationReducerEvent.PopupRaised -> {
            state.copy(errorPopup = event.popup)
        }

        IdentityVerificationReducerEvent.PopupDismissed -> {
            state.copy(errorPopup = null)
        }

        IdentityVerificationReducerEvent.ErrorConsumed -> {
            state.copy(errorMessage = null)
        }

        IdentityVerificationReducerEvent.VerifiedConsumed -> {
            state.copy(isVerified = false)
        }
    }

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
