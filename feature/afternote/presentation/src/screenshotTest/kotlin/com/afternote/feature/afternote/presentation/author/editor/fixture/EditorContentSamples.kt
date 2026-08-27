package com.afternote.feature.afternote.presentation.author.editor.fixture

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.presentation.author.editor.account.AccountSection
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection

@Composable
internal fun sampleAccountSection(): AccountSection =
    AccountSection(
        idState = rememberTextFieldState(),
        passwordState = rememberTextFieldState(),
    )

internal fun sampleRecipientSection(hasRecipient: Boolean = true): AfternoteEditorReceiverSection =
    AfternoteEditorReceiverSection(
        afternoteEditReceivers =
            if (hasRecipient) {
                listOf(
                    AfternoteEditorReceiver(
                        id = "screenshot-receiver-1",
                        name = "홍길동",
                        label = "가족",
                    ),
                )
            } else {
                emptyList()
            },
    )

internal fun sampleProcessingMethodSection(): ProcessingMethodSection =
    ProcessingMethodSection(
        items = emptyList(),
        onItemDeleteClick = {},
        onItemAdded = {},
        onItemEdited = { _, _ -> },
    )
