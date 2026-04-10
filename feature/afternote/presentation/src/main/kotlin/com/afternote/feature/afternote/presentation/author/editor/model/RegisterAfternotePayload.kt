package com.afternote.feature.afternote.presentation.author.editor.model
import com.afternote.feature.afternote.domain.model.author.ProcessingMethod

/**
 * Payload passed when the user taps "등록" on the afternote edit screen.
 * Used to build a new [com.afternote.feature.afternote.domain.model.author.ListItem] for create/update requests.
 *
 * @param atmosphere Memorial(PLAYLIST) only: "남기고 싶은 당부" text sent as playlist.atmosphere in PATCH.
 */
data class RegisterAfternotePayload(
    val serviceName: String,
    val date: String,
    val accountId: String = "",
    val password: String = "",
    val message: String = "",
    val accountProcessingMethod: String = "",
    val informationProcessingMethod: String = "",
    val processingMethods: List<ProcessingMethod> = emptyList(),
    val galleryProcessingMethods: List<ProcessingMethod> = emptyList(),
    val atmosphere: String = "",
)
