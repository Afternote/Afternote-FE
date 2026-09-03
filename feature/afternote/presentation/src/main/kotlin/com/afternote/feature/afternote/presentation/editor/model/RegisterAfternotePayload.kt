package com.afternote.feature.afternote.presentation.editor.model

import com.afternote.feature.afternote.presentation.editor.message.EditorMessageTextBlock

/**
 * Payload passed when the user taps "등록" on the afternote edit screen.
 * Used to build a new [com.afternote.feature.afternote.domain.model.author.ListItem] for create/update requests.
 *
 * @param messageBlocks "남기실 말씀" 블록들. 편집 중인 빈 블록이 섞여 있을 수 있어, 도메인으로 옮길 때 걸러진다.
 */
data class RegisterAfternotePayload(
    val serviceName: String,
    val date: String,
    val accountId: String = "",
    val password: String = "",
    val messageBlocks: List<EditorMessageTextBlock> = emptyList(),
    val processingMethods: List<String> = emptyList(),
)
