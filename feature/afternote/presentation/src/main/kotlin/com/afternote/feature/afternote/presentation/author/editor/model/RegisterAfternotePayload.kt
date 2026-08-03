package com.afternote.feature.afternote.presentation.author.editor.model
import com.afternote.feature.afternote.domain.model.author.ProcessingMethod

/**
 * Payload passed when the user taps "등록" on the afternote edit screen.
 * Used to build a new [com.afternote.feature.afternote.domain.model.author.ListItem] for create/update requests.
 *
 */
data class RegisterAfternotePayload(
    val serviceName: String,
    val date: String,
    val accountId: String = "",
    val password: String = "",
    val message: String = "",
    val processingMethods: List<ProcessingMethod> = emptyList(),
)
