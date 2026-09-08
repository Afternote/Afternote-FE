package com.afternote.feature.afternote.presentation.editor.fixture

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.presentation.editor.account.AccountSection
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodSection
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiverSection

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
                        id = 1L,
                        name = "홍길동",
                        label = "가족",
                    ),
                )
            } else {
                emptyList()
            },
        onAddClick = {},
        onItemDeleteClick = {},
    )

internal fun sampleProcessingMethodSection(): ProcessingMethodSection =
    ProcessingMethodSection(
        items = emptyList(),
        onItemDeleteClick = {},
        onItemAdded = {},
        onItemEdited = { _, _ -> },
    )
