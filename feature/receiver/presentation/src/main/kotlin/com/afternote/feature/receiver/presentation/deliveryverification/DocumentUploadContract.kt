package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopup

internal sealed interface DocumentUploadIntent : MviIntent {
    class UploadDocument(
        val slot: DocumentSlot,
        val bytes: ByteArray,
        val extension: String,
        val displayName: String,
    ) : DocumentUploadIntent

    data object DocumentReadFailed : DocumentUploadIntent

    data object Submit : DocumentUploadIntent

    data object RetryFailedRequest : DocumentUploadIntent

    data object DismissErrorPopup : DocumentUploadIntent

    data object ConsumeError : DocumentUploadIntent

    data object ConsumeSubmitted : DocumentUploadIntent
}

internal sealed interface DocumentUploadReducerEvent : ReducerEvent {
    data class UploadStarted(
        val slot: DocumentSlot,
        val displayName: String,
    ) : DocumentUploadReducerEvent

    data class Uploaded(
        val slot: DocumentSlot,
        val fileUrl: String,
    ) : DocumentUploadReducerEvent

    data class UploadRestored(
        val slot: DocumentSlot,
        val previous: DocumentSlotState,
    ) : DocumentUploadReducerEvent

    data object SubmissionStarted : DocumentUploadReducerEvent

    data object Submitted : DocumentUploadReducerEvent

    data object SubmissionFinished : DocumentUploadReducerEvent

    data class ErrorRaised(
        val message: UiText,
        val finishesSubmission: Boolean = false,
    ) : DocumentUploadReducerEvent

    data class PopupRaised(
        val popup: ReceiverErrorPopup,
    ) : DocumentUploadReducerEvent

    data object PopupDismissed : DocumentUploadReducerEvent

    data object ErrorConsumed : DocumentUploadReducerEvent

    data object SubmittedConsumed : DocumentUploadReducerEvent
}

internal fun reduceDocumentUpload(
    state: DocumentUploadUiState,
    event: DocumentUploadReducerEvent,
): DocumentUploadUiState =
    when (event) {
        is DocumentUploadReducerEvent.UploadStarted -> {
            state.updateSlot(event.slot) { it.copy(displayName = event.displayName, isUploading = true) }
        }

        is DocumentUploadReducerEvent.Uploaded -> {
            state.updateSlot(event.slot) { it.copy(fileUrl = event.fileUrl, isUploading = false) }
        }

        is DocumentUploadReducerEvent.UploadRestored -> {
            state.updateSlot(event.slot) { event.previous }
        }

        DocumentUploadReducerEvent.SubmissionStarted -> {
            state.copy(isSubmitting = true, errorMessage = null)
        }

        DocumentUploadReducerEvent.Submitted -> {
            state.copy(isSubmitting = false, isSubmitted = true)
        }

        DocumentUploadReducerEvent.SubmissionFinished -> {
            state.copy(isSubmitting = false)
        }

        is DocumentUploadReducerEvent.ErrorRaised -> {
            state.copy(
                errorMessage = event.message,
                isSubmitting =
                    state.isSubmitting && !event.finishesSubmission,
            )
        }

        is DocumentUploadReducerEvent.PopupRaised -> {
            state.copy(errorPopup = event.popup)
        }

        DocumentUploadReducerEvent.PopupDismissed -> {
            state.copy(errorPopup = null)
        }

        DocumentUploadReducerEvent.ErrorConsumed -> {
            state.copy(errorMessage = null)
        }

        DocumentUploadReducerEvent.SubmittedConsumed -> {
            state.copy(isSubmitted = false)
        }
    }

private inline fun DocumentUploadUiState.updateSlot(
    slot: DocumentSlot,
    transform: (DocumentSlotState) -> DocumentSlotState,
): DocumentUploadUiState =
    when (slot) {
        DocumentSlot.DeathCertificate -> copy(deathCertificate = transform(deathCertificate))
        DocumentSlot.FamilyRelationCertificate -> copy(familyRelationCertificate = transform(familyRelationCertificate))
    }
